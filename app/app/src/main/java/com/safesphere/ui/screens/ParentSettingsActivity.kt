package com.safesphere.ui.screens

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import com.safesphere.data.ApiClient
import com.safesphere.data.TokenStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/** Parent's settings: linked device, what is shared, and Unpair. */
class ParentSettingsActivity : Activity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val store by lazy { TokenStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        fun dp(v: Int) = UiKit.dp(ctx, v)

        val back = UiKit.roundIcon(ctx, "←", 40, "#E8EEFD", UiKit.INK, 18f).apply {
            setOnClickListener { finish() }
        }
        val titles = UiKit.vertical(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply { leftMargin = dp(12) }
            addView(UiKit.text(ctx, "Parent settings", 20f, UiKit.INK, bold = true))
            addView(UiKit.text(ctx, "Pairing & privacy", 12f, UiKit.MUTED))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(back)
            addView(titles)
        }

        // ---- linked device ----
        val linkedTop = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(Icons.circle(ctx, "smartphone", 40, Pal.CONTAINER, Pal.PRIMARY_DARK))
            addView(UiKit.vertical(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply { leftMargin = dp(12) }
                addView(UiKit.text(ctx, "Teen's phone", 16f, UiKit.INK, bold = true))
                addView(UiKit.text(ctx, "Linked with a one-time code", 12f, UiKit.MUTED))
            })
            addView(Icons.pill(ctx, "check", "Linked", Pal.GREEN_DARK, Pal.GREEN_SOFT, 11f))
        }
        val linkedCard = UiKit.card(this).apply { addView(linkedTop) }
        UiKit.topMargin(this, linkedCard, 16)

        // ---- what is shared ----
        val sharedCard = UiKit.card(this).apply {
            addView(UiKit.text(ctx, "What you can see", 17f, UiKit.INK, bold = true))
            addView(UiKit.text(
                ctx,
                "Only counts: how many sites and messages were blocked, and when. Never the websites, the messages or any content.",
                13f, UiKit.MUTED
            ).also { UiKit.topMargin(ctx, it, 6) })
            addView(UiKit.button(ctx, "View what the app sees", filled = false) {
                startActivity(Intent(ctx, TransparencyActivity::class.java))
            }.also { UiKit.topMargin(ctx, it, 12) })
        }
        UiKit.topMargin(this, sharedCard, 16)

        // ---- unpair ----
        val unpairCard = UiKit.card(this).apply {
            addView(UiKit.text(ctx, "Unpair", 17f, UiKit.INK, bold = true))
            addView(UiKit.text(
                ctx,
                "This phone will stop receiving your teen's summary, and your teen's phone goes back to setup. You can pair again with a new code.",
                13f, UiKit.MUTED
            ).also { UiKit.topMargin(ctx, it, 6) })
            addView(Icons.softButton(ctx, "x", "Unpair from teen's phone", Pal.RED_TINT, Pal.RED) {
                confirmUnpair()
            })
        }
        UiKit.topMargin(this, unpairCard, 16)

        val column = UiKit.vertical(this).apply {
            setPadding(dp(16), dp(8), dp(16), dp(24))
            addView(header)
            addView(linkedCard)
            addView(sharedCard)
            addView(unpairCard)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(UiKit.BG))
            addView(ScrollView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(UiKit.MATCH, 0, 1f)
                addView(column)
            })
        }
        UiKit.padForSystemBars(root)
        setContentView(root)
    }

    private fun confirmUnpair() {
        AlertDialog.Builder(this)
            .setTitle("Unpair from your teen's phone?")
            .setMessage("You will stop seeing their weekly summary. This can't be undone, but you can pair again later.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Unpair") { _, _ -> doUnpair() }
            .show()
    }

    private fun doUnpair() {
        val token = store.getParentToken()
        if (token == null) {
            finishUnpair()
            return
        }
        Toast.makeText(this, "Unpairing… the server can take a moment.", Toast.LENGTH_SHORT).show()
        scope.launch {
            try {
                kotlinx.coroutines.withContext(Dispatchers.IO) {
                    ApiClient.apiService.unpair(store.bearer(token))
                }
                finishUnpair()
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                if (e.code() == 401) finishUnpair()
                else Toast.makeText(this@ParentSettingsActivity, "Something went wrong (error ${e.code()}).", Toast.LENGTH_LONG).show()
            } catch (e: IOException) {
                Toast.makeText(this@ParentSettingsActivity, "Couldn't reach the server. Try again.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun finishUnpair() {
        Session.resetParent(this)
        Session.leaveToWelcome(this)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}