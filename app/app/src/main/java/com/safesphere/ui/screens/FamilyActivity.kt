package com.safesphere.ui.screens

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import com.safesphere.data.AppDatabase
import com.safesphere.data.ChartDataRepository
import com.safesphere.data.WeeklyChartData
import com.safesphere.ui.charts.HeatmapView
import com.safesphere.ui.charts.WeeklyBarChartView
import com.safesphere.ui.shield.AnimatedShieldView
import com.safesphere.ui.shield.ShieldState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Family tab: the parent's view of the teen's device.
 * Connected  -> green "Protection is ON" card.
 * No signal  -> red "Protection may be off" card, charts greyed out.
 */
class FamilyActivity : Activity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val repository by lazy {
        ChartDataRepository(AppDatabase.getInstance(this).blockedEventDao())
    }

    // TODO: drive this from the heartbeat (GET /api/status/{device_id}).
    // For the demo, long-press the "SafeSphere" title to flip between the two states.
    private var connected = true
    private var data: WeeklyChartData? = null
    private lateinit var host: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        host = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(UiKit.MATCH, 0, 1f)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(UiKit.BG))
            addView(UiKit.topBar(ctx, "Shield Dashboard") {
                connected = !connected
                Toast.makeText(
                    ctx, if (connected) "Demo: device connected" else "Demo: device disconnected",
                    Toast.LENGTH_SHORT
                ).show()
                render()
            })
            addView(host)
            addView(Nav.bar(ctx, Tab.FAMILY))
        }
        UiKit.padForSystemBars(root)
        setContentView(root)
        render()
    }

    override fun onResume() {
        super.onResume()
        scope.launch {
            data = withContext(Dispatchers.IO) { repository.getWeeklyChartData() }
            render()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun render() {
        host.removeAllViews()
        host.addView(buildColumn())
    }

    private fun buildColumn(): LinearLayout {
        val ctx = this
        val column = UiKit.vertical(this).apply {
            setPadding(UiKit.dp(ctx, 16), UiKit.dp(ctx, 8), UiKit.dp(ctx, 16), UiKit.dp(ctx, 24))
        }
        column.addView(if (connected) connectedSection() else disconnectedSection())

        val bar = WeeklyBarChartView(this)
        val heat = HeatmapView(this)
        data?.let {
            bar.setData(it.dailyCounts)
            heat.setData(it.heatmap)
        }
        val barCard = chartCard("Blocked this week", "Past 7 days", bar, 220)
        val heatCard = chartCard("When did it happen?", "Time slots vs. weekly cadence", heat, 340)
        if (!connected) {
            barCard.alpha = 0.4f
            heatCard.alpha = 0.4f
        }
        column.addView(barCard)
        column.addView(heatCard)
        return column
    }

    private fun statusRow(active: Boolean): LinearLayout {
        val ctx = this
        val label = UiKit.text(
            ctx,
            if (active) "● Device Status: Active" else "● Device Status: Inactive",
            13f,
            if (active) UiKit.GREEN_DARK else UiKit.RED,
            bold = true
        ).apply { layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f) }
        val gear = UiKit.roundIcon(ctx, "⚙", 40, UiKit.BLUE_SOFT, UiKit.BLUE_DARK, 20f).apply {
            setOnClickListener {
                Toast.makeText(ctx, "Protection settings: coming soon", Toast.LENGTH_SHORT).show()
            }
        }
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(label)
            addView(gear)
        }
    }

    private fun connectedSection(): LinearLayout {
        val ctx = this
        fun dp(v: Int) = UiKit.dp(ctx, v)
        val shield = AnimatedShieldView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(64), dp(64))
            setState(ShieldState.ON)
        }
        val texts = UiKit.vertical(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply { leftMargin = dp(12) }
            addView(UiKit.text(ctx, "Protection is ON", 20f, UiKit.INK, bold = true))
            // TODO: real time from the heartbeat
            addView(UiKit.text(ctx, "Teen's device checked in 2 min ago", 12f, UiKit.MUTED))
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(shield)
            addView(texts)
        }
        val card = UiKit.card(this).apply {
            background = UiKit.rounded(
                Color.parseColor("#EAF8F1"), dp(16).toFloat(), Color.parseColor("#CDEBDD"), dp(1)
            )
            addView(row)
            addView(UiKit.statusPills(ctx).also { UiKit.topMargin(ctx, it, 14) })
        }
        UiKit.topMargin(this, card, 12)
        return UiKit.vertical(this).apply {
            addView(statusRow(true))
            addView(card)
        }
    }

    private fun disconnectedSection(): LinearLayout {
        val ctx = this
        fun dp(v: Int) = UiKit.dp(ctx, v)

        val alertShield = AnimatedShieldView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(56), dp(56))
            setState(ShieldState.ALERT)
        }
        val badge = UiKit.pill(ctx, "Disconnected", "#FFFFFF", "#B3261E", 11f).apply {
            layoutParams = LinearLayout.LayoutParams(UiKit.WRAP, UiKit.WRAP).apply { leftMargin = dp(12) }
        }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(alertShield)
            addView(badge)
        }
        val title = UiKit.text(ctx, "Protection may be off", 22f, UiKit.RED, bold = true)
        UiKit.topMargin(ctx, title, 12)
        val body = UiKit.text(
            ctx,
            "No signal from the teen's device for 6 hours. Screen time and network shields are unresponsive.",
            13f, UiKit.INK
        )
        UiKit.topMargin(ctx, body, 6)
        val check = UiKit.button(this, "Check settings", filled = true, colorHex = UiKit.BLUE_DARK) {
            // TODO: open the protection settings screen
            Toast.makeText(ctx, "Protection settings: coming soon", Toast.LENGTH_SHORT).show()
        }

        val redCard = UiKit.card(this).apply {
            background = UiKit.rounded(
                Color.parseColor(UiKit.RED_SOFT), dp(16).toFloat(), Color.parseColor("#F5B8B2"), dp(1)
            )
            addView(top)
            addView(title)
            addView(body)
            addView(check)
        }
        UiKit.topMargin(this, redCard, 12)

        val small = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(smallCard("🔋", "Battery / Low Power", "May restrict sync", true))
            addView(smallCard("✈", "Network Offline", "Airplane mode on?", false))
        }
        UiKit.topMargin(this, small, 12)

        val note = UiKit.infoBox(
            ctx, "⏸", UiKit.text(ctx, "Analytics paused while protection is disconnected.", 12f, UiKit.INK),
            UiKit.BLUE_SOFT
        )
        UiKit.topMargin(this, note, 12)

        return UiKit.vertical(this).apply {
            addView(statusRow(false))
            addView(redCard)
            addView(small)
            addView(note)
        }
    }

    private fun smallCard(icon: String, title: String, sub: String, first: Boolean): LinearLayout {
        val ctx = this
        val texts = UiKit.vertical(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply { leftMargin = UiKit.dp(ctx, 8) }
            addView(UiKit.text(ctx, title, 12f, UiKit.INK, bold = true))
            addView(UiKit.text(ctx, sub, 11f, UiKit.MUTED))
        }
        return UiKit.card(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(UiKit.dp(ctx, 12), UiKit.dp(ctx, 12), UiKit.dp(ctx, 12), UiKit.dp(ctx, 12))
            layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply {
                if (first) rightMargin = UiKit.dp(ctx, 8)
            }
            addView(UiKit.text(ctx, icon, 18f, UiKit.MUTED))
            addView(texts)
        }
    }

    private fun chartCard(title: String, subtitle: String, chart: View, heightDp: Int): LinearLayout {
        val ctx = this
        val card = UiKit.card(ctx)
        card.addView(UiKit.text(ctx, title, 17f, UiKit.INK, bold = true))
        card.addView(UiKit.text(ctx, subtitle, 12f, UiKit.MUTED))
        chart.layoutParams = LinearLayout.LayoutParams(UiKit.MATCH, UiKit.dp(ctx, heightDp))
            .apply { topMargin = UiKit.dp(ctx, 12) }
        card.addView(chart)
        UiKit.topMargin(ctx, card, 16)
        return card
    }
}