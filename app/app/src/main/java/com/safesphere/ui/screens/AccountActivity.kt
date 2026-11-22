package com.safesphere.ui.screens

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import com.safesphere.data.TokenStore

/** User icon: who is using this phone, shortcuts, and "start setup again". */
class AccountActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        fun dp(v: Int) = UiKit.dp(ctx, v)
        val isParent = AppPrefs.getRole(ctx) == AppPrefs.ROLE_PARENT
        val store = TokenStore(ctx)

        val back = UiKit.roundIcon(ctx, "←", 40, "#E8EEFD", UiKit.INK, 18f).apply {
            setOnClickListener { finish() }
        }
        val titles = UiKit.vertical(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply { leftMargin = dp(12) }
            addView(UiKit.text(ctx, "Account", 20f, UiKit.INK, bold = true))
            addView(UiKit.text(ctx, "This phone", 12f, UiKit.MUTED))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
            addView(back)
            addView(titles)
        }

        // ---- who is this ----
        val linked = if (isParent) store.getParentToken() != null else store.getTeenToken() != null
        val who = UiKit.card(this).apply {
            addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(Icons.circle(ctx, "user", 52, "#6056F0", "#FFFFFF"))
                addView(UiKit.vertical(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply { leftMargin = dp(14) }
                    addView(UiKit.text(ctx, if (isParent) "Parent" else "Teen", 20f, UiKit.INK, bold = true))
                    addView(UiKit.text(
                        ctx,
                        if (isParent) "Sees counts and status of your teen's phone"
                        else "Protected by SafeSphere on this phone",
                        12f, UiKit.MUTED
                    ))
                })
                addView(
                    if (linked) Icons.pill(ctx, "check", "Linked", Pal.GREEN_DARK, Pal.GREEN_SOFT, 11f)
                    else Icons.pill(ctx, "alert", "Not linked", Pal.RED, Pal.RED_SOFT, 11f)
                )
            })
        }
        UiKit.topMargin(this, who, 8)

        // ---- shortcuts ----
        val links = UiKit.card(this).apply {
            addView(UiKit.text(ctx, "Shortcuts", 17f, UiKit.INK, bold = true))
            addView(
                if (isParent) Icons.softButton(ctx, "users", "Parent settings", Pal.CONTAINER, Pal.PRIMARY_DARK) {
                    startActivity(Intent(ctx, ParentSettingsActivity::class.java))
                } else Icons.softButton(ctx, "shield", "Protection settings", Pal.CONTAINER, Pal.PRIMARY_DARK) {
                    startActivity(Intent(ctx, ProtectionSettingsActivity::class.java))
                }
            )
            addView(Icons.softButton(ctx, "bell", "Alerts", Pal.CONTAINER, Pal.PRIMARY_DARK) {
                startActivity(Intent(ctx, AlertsActivity::class.java))
            })
            addView(Icons.softButton(ctx, "eye", "What does this app see?", Pal.CONTAINER, Pal.PRIMARY_DARK) {
                startActivity(Intent(ctx, TransparencyActivity::class.java))
            })
        }
        UiKit.topMargin(this, links, 16)

        // ---- start again (demo helper) ----
        val reset = UiKit.card(this).apply {
            addView(UiKit.text(ctx, "Start setup again", 17f, UiKit.INK, bold = true))
            addView(UiKit.text(
                ctx,
                "Forgets the role and pairing on this phone. You will need to pair again.",
                13f, UiKit.MUTED
            ).also { UiKit.topMargin(ctx, it, 6) })
            addView(Icons.softButton(ctx, "swap", "Reset this phone's setup", Pal.RED_TINT, Pal.RED) {
                AlertDialog.Builder(ctx)
                    .setTitle("Start setup again?")
                    .setMessage("This phone forgets its role and pairing. The other phone will show no signal until you pair again.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Reset") { _, _ ->
                        Session.resetTeen(ctx)
                        store.resetParentState()
                        AppPrefs.setProtectionOn(ctx, true)
                        Session.leaveToWelcome(this@AccountActivity)
                    }
                    .show()
            })
        }
        UiKit.topMargin(this, reset, 16)

        val column = UiKit.vertical(this).apply {
            setPadding(dp(16), dp(8), dp(16), dp(24))
            addView(who)
            addView(links)
            addView(reset)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(UiKit.BG))
            addView(header)
            addView(ScrollView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(UiKit.MATCH, 0, 1f)
                addView(column)
            })
        }
        UiKit.padForSystemBars(root)
        setContentView(root)
    }
}