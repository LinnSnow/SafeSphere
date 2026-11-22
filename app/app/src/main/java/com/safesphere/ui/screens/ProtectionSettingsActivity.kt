package com.safesphere.ui.screens

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Protection settings (opened from the gear on Home).
 * Everything that weakens protection needs the parent PIN first.
 */
class ProtectionSettingsActivity : Activity() {

    private var unlocked = false
    private var entry = ""

    private lateinit var boxes: CodeBoxes
    private lateinit var pinError: TextView
    private lateinit var pinArea: LinearLayout
    private lateinit var authOk: LinearLayout
    private lateinit var offHolder: LinearLayout
    private lateinit var pauseHolder: LinearLayout
    private lateinit var statusDot: View
    private lateinit var statusTitle: TextView
    private lateinit var statusSub: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        fun dp(v: Int) = UiKit.dp(ctx, v)

        // ---- header ----
        val back = Icons.circle(ctx, "arrow-left", 40, Pal.CONTAINER, Pal.INK).apply {
            setOnClickListener { finish() }
        }
        val title = UiKit.text(ctx, "Protection settings", 19f, Pal.INK, bold = true).apply {
            layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply { leftMargin = dp(12) }
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(back)
            addView(title)
            addView(Icons.circle(ctx, "user", 36, Pal.ACCENT, "#FFFFFF"))
        }

        // ---- status strip ----
        statusDot = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dp(10), dp(10)).apply { rightMargin = dp(12) }
        }
        statusTitle = UiKit.text(ctx, "", 14f, Pal.INK, bold = true)
        statusSub = UiKit.text(ctx, "", 12f, Pal.MUTED)
        val statusTexts = UiKit.vertical(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f)
            addView(statusTitle)
            addView(statusSub)
        }
        val statusStrip = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = UiKit.rounded(Color.WHITE, dp(14).toFloat(), Color.parseColor(Pal.BORDER), dp(1))
            setPadding(dp(14), dp(12), dp(14), dp(12))
            addView(statusDot)
            addView(statusTexts)
            addView(Icons.icon(ctx, "shield-check", 20, Pal.GREEN_DARK))
        }
        UiKit.topMargin(ctx, statusStrip, 16)

        // ---- parent authorization card ----
        val authHead = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(Icons.circle(ctx, "lock", 44, Pal.CONTAINER, Pal.PRIMARY_DARK))
            addView(UiKit.vertical(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply { leftMargin = dp(12) }
                addView(UiKit.text(ctx, "Parent authorization required", 16f, Pal.INK, bold = true))
                addView(UiKit.text(
                    ctx,
                    "Enter the ${SetPinActivity.PIN_LENGTH}-digit master passcode to adjust web filters or deactivate SafeSphere.",
                    12f, Pal.MUTED
                ).apply { setPadding(0, dp(2), 0, 0) })
            })
        }

        boxes = CodeBoxes(ctx, SetPinActivity.PIN_LENGTH, masked = true)
        pinError = UiKit.text(ctx, "", 13f, Pal.RED, bold = true).apply {
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        val panel = UiKit.vertical(ctx).apply {
            background = UiKit.rounded(Color.parseColor(Pal.SURFACE), dp(14).toFloat())
            setPadding(dp(14), dp(14), dp(14), dp(14))
            addView(UiKit.text(ctx, "ENTER PARENT PIN", 11f, Pal.MUTED, bold = true).apply {
                gravity = Gravity.CENTER
            })
            addView(UiKit.vertical(ctx).apply { addView(boxes.view) }.also { UiKit.topMargin(ctx, it, 10) })
            addView(pinError.also { UiKit.topMargin(ctx, it, 8) })
        }
        val pad = Widgets.keypad(
            ctx,
            onDigit = { d ->
                if (!unlocked && entry.length < SetPinActivity.PIN_LENGTH) {
                    entry += d
                    boxes.setValue(entry)
                    clearPinError()
                    if (entry.length == SetPinActivity.PIN_LENGTH) verifyPin()
                }
            },
            onDelete = {
                if (!unlocked && entry.isNotEmpty()) {
                    entry = entry.dropLast(1)
                    boxes.setValue(entry)
                    clearPinError()
                }
            }
        )
        UiKit.topMargin(ctx, pad, 12)
        pinArea = UiKit.vertical(ctx).apply {
            addView(panel.also { UiKit.topMargin(ctx, it, 14) })
            addView(pad)
        }

        authOk = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = UiKit.rounded(Color.parseColor(Pal.GREEN_TINT), dp(12).toFloat())
            setPadding(dp(12), dp(10), dp(12), dp(10))
            visibility = View.GONE
            addView(Icons.icon(ctx, "check", 16, Pal.GREEN_DARK).apply {
                (layoutParams as LinearLayout.LayoutParams).rightMargin = dp(8)
            })
            addView(UiKit.text(ctx, "Parent verified. You can change protection now.", 13f, Pal.GREEN_DARK, bold = true))
        }
        UiKit.topMargin(ctx, authOk, 14)

        offHolder = UiKit.vertical(ctx)

        val authCard = UiKit.card(ctx).apply {
            addView(authHead)
            addView(pinArea)
            addView(authOk)
            addView(offHolder)
        }
        UiKit.topMargin(ctx, authCard, 16)

        // ---- temporary suspension (rebuilt in refresh()) ----
        pauseHolder = UiKit.vertical(ctx)
        UiKit.topMargin(ctx, pauseHolder, 16)

        // ---- why PIN protection ----
        fun checkRow(text: String): LinearLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(Icons.circle(ctx, "check", 22, Pal.GREEN_SOFT, Pal.GREEN_DARK))
            addView(UiKit.text(ctx, text, 13f, Pal.INK, bold = true).apply {
                layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply { leftMargin = dp(10) }
            })
        }
        val whyCard = UiKit.card(ctx).apply {
            addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(Icons.icon(ctx, "shield-check", 20, Pal.PRIMARY_DARK).apply {
                    (layoutParams as LinearLayout.LayoutParams).rightMargin = dp(8)
                })
                addView(UiKit.text(ctx, "Why PIN protection is enabled", 15f, Pal.INK, bold = true))
            })
            addView(UiKit.text(
                ctx,
                "Protection boundaries work best when shared transparently. Master passcode confirmation ensures " +
                        "adjustments are made collaboratively between you and your guardian, preventing accidental " +
                        "deactivations or malware interference.",
                13f, Pal.MUTED
            ).also { UiKit.topMargin(ctx, it, 10) })
            listOf(
                "Web filter keeps adult content and known phishing off devices",
                "Emergency contacts remain accessible even during calm hours",
                "Tamper alerts safeguard system configuration"
            ).forEach { addView(checkRow(it).also { r -> UiKit.topMargin(ctx, r, 10) }) }
        }
        UiKit.topMargin(ctx, whyCard, 16)

        // ---- mutual visibility ----
        val mutual = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = UiKit.rounded(Color.parseColor(Pal.SURFACE), dp(16).toFloat())
            setPadding(dp(14), dp(14), dp(14), dp(14))
            addView(Icons.icon(ctx, "eye", 22, Pal.MUTED).apply {
                (layoutParams as LinearLayout.LayoutParams).rightMargin = dp(12)
            })
            addView(UiKit.vertical(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f)
                addView(UiKit.text(ctx, "Mutual Visibility", 14f, Pal.INK, bold = true))
                addView(UiKit.text(ctx, "See what your guardian sees", 12f, Pal.MUTED))
            })
            addView(UiKit.pill(ctx, "View Shared Log", Pal.PRIMARY_DARK, Pal.HIGH, 12f).apply {
                setOnClickListener { startActivity(Intent(ctx, TransparencyActivity::class.java)) }
            })
        }
        UiKit.topMargin(ctx, mutual, 16)

        val column = UiKit.vertical(this).apply {
            setPadding(dp(16), dp(8), dp(16), dp(24))
            addView(header)
            addView(statusStrip)
            addView(authCard)
            addView(pauseHolder)
            addView(whyCard)
            addView(mutual)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(Pal.BG))
            addView(ScrollView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(UiKit.MATCH, 0, 1f)
                addView(column)
            })
        }
        UiKit.padForSystemBars(root)
        setContentView(root)
        refresh()
    }

    override fun onStart() {
        super.onStart()
        refresh()
    }

    override fun onStop() {
        super.onStop()
        // Leaving the screen locks it again.
        unlocked = false
        entry = ""
    }

    // ---------- PIN ----------

    private fun verifyPin() {
        val ok = !AppPrefs.hasPin(this) || AppPrefs.checkPin(this, entry)
        entry = ""
        boxes.setValue("")
        if (ok) {
            unlocked = true
            refresh()
        } else {
            pinError.text = "That PIN isn't right. Try again."
            pinError.visibility = View.VISIBLE
            boxes.setError(true)
        }
    }

    private fun clearPinError() {
        pinError.visibility = View.GONE
        boxes.setError(false)
    }

    // ---------- redraw everything that depends on state ----------

    private fun refresh() {
        val ctx = this
        val on = AppPrefs.isProtectionOn(ctx)
        val paused = AppPrefs.isPaused(ctx)

        when {
            !on -> {
                statusTitle.text = "SafeSphere Shield is off"
                statusSub.text = "Web and message protection is not running."
                setDot(Pal.RED)
            }
            paused -> {
                val until = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(AppPrefs.pausedUntil(ctx)))
                statusTitle.text = "SafeSphere Shield is paused"
                statusSub.text = "Protection resumes automatically at $until."
                setDot("#F59E0B")
            }
            else -> {
                statusTitle.text = "SafeSphere Shield is Active"
                statusSub.text = "Shared account with your family"
                setDot(Pal.GREEN_DARK)
            }
        }

        pinArea.visibility = if (unlocked) View.GONE else View.VISIBLE
        authOk.visibility = if (unlocked) View.VISIBLE else View.GONE

        // "Turn protection off" / "Turn protection on"
        offHolder.removeAllViews()
        if (!on) {
            offHolder.addView(UiKit.button(ctx, "Turn protection on", filled = true) {
                AppPrefs.setProtectionOn(ctx, true)
                refresh()
            })
        } else {
            val off = Icons.softButton(
                ctx, if (unlocked) "power" else "lock", "Turn protection off", Pal.RED_TINT, Pal.RED
            ) {
                if (!unlocked) toast("Enter the parent PIN first.") else confirmTurnOff()
            }
            off.alpha = if (unlocked) 1f else 0.55f
            offHolder.addView(off)
        }

        // Temporary suspension (only while protection is on)
        pauseHolder.removeAllViews()
        if (on) pauseHolder.addView(suspensionCard(paused))
    }

    private fun suspensionCard(paused: Boolean): LinearLayout {
        val ctx = this
        fun dp(v: Int) = UiKit.dp(ctx, v)
        return UiKit.card(ctx).apply {
            addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(Icons.circle(ctx, "hourglass", 36, Pal.GREEN_SOFT, Pal.GREEN_DARK))
                addView(UiKit.text(ctx, "Temporary Suspension", 15f, Pal.INK, bold = true).apply {
                    layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply { leftMargin = dp(12) }
                })
                addView(UiKit.pill(ctx, "Safe Mode", Pal.GREEN_DARK, Pal.GREEN_SOFT, 11f))
            })
            addView(UiKit.text(
                ctx, "Request a 60-minute window for homework research or filtered app usage.", 13f, Pal.MUTED
            ).also { UiKit.topMargin(ctx, it, 10) })

            if (paused) {
                val resume = Icons.softButton(ctx, "shield-check", "Resume protection now", Pal.CONTAINER, Pal.PRIMARY_DARK) {
                    AppPrefs.clearPause(ctx)
                    refresh()
                }
                addView(resume)
            } else {
                val pause = Icons.softButton(ctx, "clock", "Pause for 1 hour", Pal.CONTAINER, Pal.PRIMARY_DARK) {
                    if (!unlocked) {
                        toast("Enter the parent PIN first.")
                    } else {
                        AppPrefs.pauseFor(ctx, 60)
                        // TODO: tell the parent's phone (backend) that protection was paused.
                        refresh()
                    }
                }
                pause.alpha = if (unlocked) 1f else 0.55f
                addView(pause)
            }

            addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(Icons.icon(ctx, "mail", 14, Pal.OUTLINE).apply {
                    (layoutParams as LinearLayout.LayoutParams).rightMargin = dp(6)
                })
                addView(UiKit.text(ctx, "Your parent will be notified immediately.", 11f, Pal.MUTED))
            }.also { UiKit.topMargin(ctx, it, 12) })
        }
    }

    private fun confirmTurnOff() {
        AlertDialog.Builder(this)
            .setTitle("Turn protection off?")
            .setMessage("SafeSphere will stop filtering until you turn it back on. Your parent will see that protection is off.")
            .setPositiveButton("Turn off") { _, _ ->
                AppPrefs.setProtectionOn(this, false)
                // TODO: tell Person 1's VPN service and Person 4's ProtectionState.
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setDot(colorHex: String) {
        statusDot.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(colorHex))
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}