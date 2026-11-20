package com.safesphere.ui.screens

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.safesphere.data.ApiClient
import com.safesphere.data.PairConfirmRequest
import com.safesphere.data.TokenStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/**
 * Parent's phone: types the 6-digit code shown on the teen's phone.
 * The SERVER decides: 404 = invalid or already used, 400 = expired, 503 = try again, IOException = no connection.
 */
class PairActivity : Activity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val store by lazy { TokenStore(this) }
    private val codeLength = 6
    private var code = ""
    private var busy = false

    private lateinit var boxes: CodeBoxes
    private lateinit var errorText: TextView
    private lateinit var waitNote: TextView
    private lateinit var pairButton: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        fun dp(v: Int) = UiKit.dp(ctx, v)

        val back = UiKit.roundIcon(ctx, "←", 40, "#E8EEFD", UiKit.INK, 18f).apply {
            setOnClickListener { finish() }
        }
        val icon = UiKit.roundIcon(ctx, "🛡", 72, UiKit.BLUE_SOFT, UiKit.BLUE_DARK, 30f).apply {
            (layoutParams as LinearLayout.LayoutParams).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(4)
            }
        }
        val title = UiKit.text(ctx, "Pair with Teen Device", 24f, UiKit.INK, bold = true)
            .apply { gravity = Gravity.CENTER }
        UiKit.topMargin(ctx, title, 16)
        val subtitle = UiKit.text(
            ctx, "Enter the 6-digit code shown on the other device to link safely.", 14f, UiKit.MUTED
        ).apply { gravity = Gravity.CENTER }
        UiKit.topMargin(ctx, subtitle, 6)

        // ---- code card ----
        boxes = CodeBoxes(ctx, codeLength, masked = false)
        errorText = UiKit.text(ctx, "", 13f, UiKit.RED, bold = true).apply {
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        UiKit.topMargin(ctx, errorText, 12)
        val expiry = UiKit.text(ctx, "⏱ Codes expire after 10 minutes for your safety.", 12f, UiKit.MUTED)
            .apply { gravity = Gravity.CENTER }
        UiKit.topMargin(ctx, expiry, 12)
        val pad = Widgets.keypad(
            ctx,
            onDigit = { d ->
                if (!busy && code.length < codeLength) {
                    code += d
                    boxes.setValue(code)
                    clearError()
                }
            },
            onDelete = {
                if (!busy && code.isNotEmpty()) {
                    code = code.dropLast(1)
                    boxes.setValue(code)
                    clearError()
                }
            }
        )
        UiKit.topMargin(ctx, pad, 12)
        val codeCard = UiKit.card(this).apply {
            addView(boxes.view)
            addView(errorText)
            addView(expiry)
            addView(pad)
        }
        UiKit.topMargin(this, codeCard, 20)

        // ---- transparent sync note ----
        val syncTexts = UiKit.vertical(this).apply {
            addView(UiKit.text(ctx, "TRANSPARENT SYNC", 11f, UiKit.GREEN_DARK, bold = true))
            addView(UiKit.text(ctx, "Clear permissions, shared controls", 14f, UiKit.INK, bold = true))
            addView(UiKit.text(ctx, "Both devices preview active protection rules openly.", 12f, UiKit.MUTED))
        }
        val syncBox = UiKit.infoBox(ctx, "🔄", syncTexts, "#EEF2FE")
        UiKit.topMargin(this, syncBox, 16)

        pairButton = UiKit.button(this, "Pair devices", filled = true) { submit() }
        waitNote = UiKit.text(
            ctx, "Connecting… the server can take up to a minute to wake up.", 12f, UiKit.MUTED
        ).apply {
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        UiKit.topMargin(ctx, waitNote, 8)

        val help = UiKit.text(ctx, "Need help finding the code? Tap here", 13f, UiKit.BLUE_DARK, bold = true)
            .apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(12), 0, dp(12))
                setOnClickListener {
                    AlertDialog.Builder(ctx)
                        .setTitle("Where do I find the code?")
                        .setMessage(
                            "On the teen's phone, open SafeSphere and choose \"I'm the teen\". " +
                                    "The 6-digit code is shown there and works for 10 minutes. " +
                                    "If it stops working, ask for a new code."
                        )
                        .setPositiveButton("Got it", null)
                        .show()
                }
            }
        UiKit.topMargin(ctx, help, 4)

        val column = UiKit.vertical(this).apply {
            setPadding(dp(16), dp(8), dp(16), dp(24))
            addView(back)
            addView(icon)
            addView(title)
            addView(subtitle)
            addView(codeCard)
            addView(syncBox)
            addView(pairButton)
            addView(waitNote)
            addView(help)
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

    private fun submit() {
        if (busy) return
        if (code.length < codeLength) {
            showError("Enter all 6 digits of the code.")
            return
        }
        busy = true
        pairButton.text = "Connecting…"
        pairButton.alpha = 0.6f
        waitNote.visibility = View.VISIBLE
        scope.launch {
            try {
                val res = ApiClient.apiService.pairConfirm(PairConfirmRequest(code))
                store.savePairing(res.deviceId, res.parentToken)
                onPaired()
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                when (e.code()) {
                    404 -> showError("That code isn't valid or has already been used. Ask for a new code and try again.")
                    400 -> showError("That code has expired. Codes last 10 minutes, so ask for a new one.")
                    503 -> showError("The server couldn't prepare the link. Please try again.")
                    else -> showError("Something went wrong (error ${e.code()}). Please try again.")
                }
            } catch (e: IOException) {
                showError("Couldn't reach the server. Check your internet and try again.")
            } finally {
                busy = false
                pairButton.text = "Pair devices"
                pairButton.alpha = 1f
                waitNote.visibility = View.GONE
            }
        }
    }

    private fun onPaired() {
        AppPrefs.setSetupDone(this, true)
        Toast.makeText(this, "Devices paired", Toast.LENGTH_SHORT).show()
        startActivity(
            Intent(this, FamilyActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
        boxes.setError(true)
    }

    private fun clearError() {
        errorText.visibility = View.GONE
        boxes.setError(false)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}