package com.safesphere.ui.screens

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.ScrollView

/**
 * "What does this app see?"  Plain, honest list for the teen.
 * Edit the two lists below if the team's features change.
 */
class TransparencyActivity : Activity() {

    private val weCheck = listOf(
        "Website names, to block adult sites",
        "Message previews, to warn you about adult language",
        // TODO: keep this line only if photo scanning is confirmed with the team
        "New photos, to flag adult images"
    )

    private val neverSee = listOf(
        "Your passwords",
        "What you type in other apps",
        "Your call logs",
        "Your location"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        fun dp(v: Int) = UiKit.dp(ctx, v)

        val back = UiKit.text(this, "← Back", 16f, UiKit.BLUE, bold = true).apply {
            setPadding(0, dp(8), dp(16), dp(8))
            setOnClickListener { finish() }
        }

        val checkCard = UiKit.card(this).apply {
            addView(UiKit.text(ctx, "What we check", 18f, bold = true))
            weCheck.forEach { addView(UiKit.checkLine(ctx, "✔", UiKit.GREEN, it)) }
        }
        UiKit.topMargin(this, checkCard, 20)

        val note = UiKit.text(
            this,
            "We never save your messages, photos or browsing history. Only counts.",
            14f, UiKit.MUTED
        ).apply { setPadding(dp(4), dp(12), dp(4), 0) }

        val neverCard = UiKit.card(this).apply {
            addView(UiKit.text(ctx, "What we never see", 18f, bold = true))
            neverSee.forEach { addView(UiKit.checkLine(ctx, "✖", UiKit.RED, it)) }
        }
        UiKit.topMargin(this, neverCard, 20)

        val column = UiKit.vertical(this).apply {
            setPadding(dp(16), dp(48), dp(16), dp(32))
            addView(back)
            addView(UiKit.text(ctx, "What does this app see?", 26f, bold = true))
            addView(UiKit.text(ctx, "Here is exactly what SafeSphere checks, and what it never sees.", 14f, UiKit.MUTED)
                .apply { setPadding(0, dp(6), 0, 0) })
            addView(checkCard)
            addView(note)
            addView(neverCard)
        }

        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.parseColor(UiKit.BG))
            addView(column)
        })
    }
}