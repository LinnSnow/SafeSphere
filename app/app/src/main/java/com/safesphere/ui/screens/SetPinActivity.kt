package com.safesphere.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Parent sets a 6-digit PIN on the teen's phone. It is needed later to turn protection off or pause it.
 * Two steps: enter the PIN, then enter it again to confirm. Saved as a salted hash.
 */
class SetPinActivity : Activity() {

    companion object {
        const val PIN_LENGTH = 6   // change this one number to change the PIN length
    }

    private var firstPin: String? = null
    private var entry = ""

    private lateinit var boxes: CodeBoxes
    private lateinit var title: TextView
    private lateinit var subtitle: TextView
    private lateinit var errorText: TextView
    private lateinit var pinButton: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        fun dp(v: Int) = UiKit.dp(ctx, v)

        val back = UiKit.roundIcon(ctx, "←", 40, "#E8EEFD", UiKit.INK, 18f).apply {
            setOnClickListener { finish() }
        }
        val pills = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(UiKit.pill(ctx, "● Shield Protection Active", UiKit.GREEN_DARK, UiKit.GREEN_SOFT, 11f))
            addView(UiKit.text(ctx, "Guardian Level", 11f, UiKit.MUTED, bold = true).apply {
                setPadding(dp(10), 0, 0, 0)
            })
        }
        UiKit.topMargin(ctx, pills, 16)

        val icon = UiKit.roundIcon(ctx, "🔑", 64, UiKit.BLUE_SOFT, UiKit.BLUE_DARK, 26f).apply {
            (layoutParams as LinearLayout.LayoutParams).gravity = Gravity.CENTER_HORIZONTAL
        }
        UiKit.topMargin(ctx, icon, 8)
        (icon.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.CENTER_HORIZONTAL

        title = UiKit.text(ctx, "", 24f, UiKit.INK, bold = true).apply { gravity = Gravity.CENTER }
        UiKit.topMargin(ctx, title, 14)
        subtitle = UiKit.text(ctx, "", 14f, UiKit.MUTED).apply { gravity = Gravity.CENTER }
        UiKit.topMargin(ctx, subtitle, 6)

        boxes = CodeBoxes(ctx, PIN_LENGTH, masked = true)
        val boxesHolder = UiKit.vertical(ctx).apply { addView(boxes.view) }
        UiKit.topMargin(ctx, boxesHolder, 20)
        errorText = UiKit.text(ctx, "", 13f, UiKit.RED, bold = true).apply {
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        UiKit.topMargin(ctx, errorText, 10)

        val safeguard = UiKit.vertical(ctx).apply {
            addView(UiKit.text(ctx, "Guardian Safeguard", 13f, UiKit.INK, bold = true))
            addView(UiKit.text(
                ctx, "This PIN is needed to turn protection off or pause safety filters.", 12f, UiKit.MUTED
            ))
        }
        val safeBox = UiKit.infoBox(ctx, "🔒", safeguard, "#F0F3FF")
        UiKit.topMargin(ctx, safeBox, 16)

        val pad = Widgets.keypad(
            ctx,
            onDigit = { d ->
                if (entry.length < PIN_LENGTH) {
                    entry += d
                    boxes.setValue(entry)
                    clearError()
                }
            },
            onDelete = {
                if (entry.isNotEmpty()) {
                    entry = entry.dropLast(1)
                    boxes.setValue(entry)
                    clearError()
                }
            }
        )
        UiKit.topMargin(ctx, pad, 12)

        pinButton = UiKit.button(this, "", filled = true) { confirm() }

        val column = UiKit.vertical(this).apply {
            setPadding(dp(16), dp(8), dp(16), dp(24))
            addView(back)
            addView(pills)
            addView(icon)
            addView(title)
            addView(subtitle)
            addView(boxesHolder)
            addView(errorText)
            addView(safeBox)
            addView(pad)
            addView(pinButton)
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
        showFirstStage()
    }

    private fun showFirstStage() {
        firstPin = null
        entry = ""
        boxes.setValue("")
        title.text = "Set a $PIN_LENGTH-digit parent PIN"
        subtitle.text = "Choose a PIN that only the parent knows."
        pinButton.text = "Next  →"
    }

    private fun confirm() {
        if (entry.length < PIN_LENGTH) {
            showError("Enter all $PIN_LENGTH digits.")
            return
        }
        val first = firstPin
        when {
            first == null -> {
                firstPin = entry
                entry = ""
                boxes.setValue("")
                title.text = "Confirm your PIN"
                subtitle.text = "Enter the same PIN once more."
                pinButton.text = "Confirm PIN  ✓"
            }
            first == entry -> {
                AppPrefs.savePin(this, entry)
                AppPrefs.setSetupDone(this, true)
                Toast.makeText(this, "Parent PIN saved. SafeSphere is ready.", Toast.LENGTH_SHORT).show()
                startActivity(
                    Intent(this, HomeActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
            }
            else -> {
                showFirstStage()
                showError("The PINs didn't match. Please start again.")
            }
        }
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
}