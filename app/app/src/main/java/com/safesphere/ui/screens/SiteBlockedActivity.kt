package com.safesphere.ui.screens

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView

/**
 * "This site is blocked". Shown by the VPN service when it stops a page.
 * It deliberately receives NO web address, so nothing about the page can be shown or stored.
 */
class SiteBlockedActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        fun dp(v: Int) = UiKit.dp(ctx, v)

        // ---- top pill ----
        val topPill = Icons.pill(ctx, "", "● SafeSphere Shield • Active Protection", Pal.PRIMARY_DARK, Pal.CONTAINER, 11f)
            .apply { (layoutParams as LinearLayout.LayoutParams).gravity = Gravity.CENTER_HORIZONTAL }

        // ---- hero ----
        val inner = Icons.circle(ctx, "shield-check", 84, Pal.PRIMARY_DARK, "#FFFFFF").apply {
            layoutParams = FrameLayout.LayoutParams(dp(84), dp(84), Gravity.CENTER)
        }
        val hero = FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dp(124), dp(124)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(24)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(Pal.SURFACE))
            }
            addView(inner)
        }
        val secure = Icons.pill(ctx, "lock", "Private & Secure", Pal.GREEN_DARK, "#8DF8BD", 11f).apply {
            (layoutParams as LinearLayout.LayoutParams).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(-14)
            }
        }

        val title = UiKit.text(ctx, "This site is blocked", 28f, Pal.INK, bold = true).apply { gravity = Gravity.CENTER }
        UiKit.topMargin(ctx, title, 16)
        val subtitle = UiKit.text(
            ctx, "SafeSphere stepped in to maintain a mindful and secure digital space.", 14f, Pal.MUTED
        ).apply { gravity = Gravity.CENTER }
        UiKit.topMargin(ctx, subtitle, 6)

        // ---- why card ----
        val whyCard = UiKit.card(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(Icons.circle(ctx, "shield", 40, Pal.CONTAINER, Pal.PRIMARY_DARK))
            addView(UiKit.vertical(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply { leftMargin = dp(12) }
                addView(UiKit.text(ctx, "SafeSphere blocked this to keep you safe", 14f, Pal.INK, bold = true))
                addView(UiKit.text(
                    ctx,
                    "Only a count is saved to your account health overview, never the site URL or search term.",
                    12f, Pal.MUTED
                ).apply { setPadding(0, dp(4), 0, 0) })
            })
        }
        UiKit.topMargin(ctx, whyCard, 20)

        // ---- zero history box ----
        val zero = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = UiKit.rounded(Color.parseColor(Pal.SURFACE), dp(14).toFloat())
            setPadding(dp(14), dp(14), dp(14), dp(14))
            addView(Icons.icon(ctx, "eye-off", 22, Pal.PRIMARY_DARK).apply {
                (layoutParams as LinearLayout.LayoutParams).rightMargin = dp(12)
            })
            addView(UiKit.vertical(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f)
                addView(UiKit.text(ctx, "Zero browsing history logged", 13f, Pal.INK, bold = true))
                addView(UiKit.text(
                    ctx, "Neither parents nor SafeSphere see the URL or page details.", 12f, Pal.MUTED
                ).apply { setPadding(0, dp(2), 0, 0) })
            })
        }
        UiKit.topMargin(ctx, zero, 12)

        // ---- shared transparency ----
        fun cell(label: String, big: String, small: String, first: Boolean) = UiKit.vertical(ctx).apply {
            background = UiKit.rounded(Color.parseColor(Pal.SURFACE), dp(12).toFloat())
            setPadding(dp(12), dp(12), dp(12), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply {
                if (first) rightMargin = dp(8)
            }
            addView(UiKit.text(ctx, label, 10f, Pal.MUTED, bold = true).apply { gravity = Gravity.CENTER })
            addView(UiKit.text(ctx, big, 13f, Pal.INK, bold = true).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(6), 0, dp(2))
            })
            addView(UiKit.text(ctx, small, 11f, Pal.MUTED).apply { gravity = Gravity.CENTER })
        }
        val shared = UiKit.card(ctx).apply {
            addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(UiKit.text(ctx, "Shared Transparency", 13f, Pal.INK, bold = true).apply {
                    layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f)
                })
                addView(Icons.pill(ctx, "check", "Mutual Sync", Pal.GREEN_DARK, Pal.GREEN_SOFT, 11f))
            })
            addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(cell("WHAT YOU SEE", "Filter active", "URL is kept private", true))
                addView(cell("WHAT PARENT SEES", "Shield protected", "No domain listed", false))
            }.also { UiKit.topMargin(ctx, it, 12) })
        }
        UiKit.topMargin(ctx, shared, 12)

        // ---- buttons ----
        val goBack = Icons.softButton(ctx, "arrow-left", "Go back", Pal.PRIMARY, "#FFFFFF") { finish() }
        val why = Icons.softButton(ctx, "info", "Why was this blocked?", "#FFFFFF", Pal.INK) {
            AlertDialog.Builder(ctx)
                .setTitle("Why was this blocked?")
                .setMessage(
                    "SafeSphere compares the website's name with a list of adult and harmful sites, on this phone. " +
                            "If the name is on the list, the page is stopped before it loads.\n\n" +
                            "Only the number of blocked sites is saved. The site's name is never stored or sent."
                )
                .setPositiveButton("Got it", null)
                .show()
        }.apply { elevation = dp(2).toFloat() }

        val column = UiKit.vertical(this).apply {
            setPadding(dp(16), dp(16), dp(16), dp(24))
            addView(topPill)
            addView(hero)
            addView(secure)
            addView(title)
            addView(subtitle)
            addView(whyCard)
            addView(zero)
            addView(shared)
            addView(goBack)
            addView(why)
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
    }
}