package com.safesphere.ui.screens

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.safesphere.data.AppDatabase
import com.safesphere.data.ChartDataRepository
import com.safesphere.data.WeeklyChartData
import com.safesphere.ui.charts.HeatmapView
import com.safesphere.ui.charts.TrendLineChartView
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
 * Home ("Shield dashboard"): status card, weekly charts, summary line,
 * Mutual Visibility note and a button to the transparency screen.
 * Chart numbers come ONLY from ChartDataRepository.getWeeklyChartData().
 */
class HomeActivity : Activity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val repository by lazy {
        ChartDataRepository(AppDatabase.getInstance(this).blockedEventDao())
    }

    private lateinit var barChart: WeeklyBarChartView
    private lateinit var trendChart: TrendLineChartView
    private lateinit var heatmap: HeatmapView
    private lateinit var summary: TextView
    private lateinit var totalPill: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        fun dp(v: Int) = UiKit.dp(ctx, v)

        // ---- status card ----
        val shield = AnimatedShieldView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(64), dp(64))
            setState(ShieldState.ON)
        }
        val gear = UiKit.roundIcon(ctx, "⚙", 40, UiKit.BLUE_SOFT, UiKit.BLUE_DARK, 20f).apply {
            // TODO: open ProtectionSettingsActivity (PIN-gated) once it exists
            setOnClickListener {
                Toast.makeText(ctx, "Protection settings: coming soon", Toast.LENGTH_SHORT).show()
            }
        }
        val statusTexts = UiKit.vertical(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply { leftMargin = dp(12) }
            addView(UiKit.text(ctx, "Protection is ON", 20f, UiKit.INK, bold = true))
            // TODO: real "last checked" time from the heartbeat (GET /api/status/{device_id})
            addView(UiKit.text(ctx, "Last checked 2 min ago", 12f, UiKit.MUTED))
        }
        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(shield)
            addView(statusTexts)
            addView(gear)
        }
        val statusCard = UiKit.card(this).apply {
            background = UiKit.rounded(
                Color.parseColor("#EAF8F1"), dp(16).toFloat(), Color.parseColor("#CDEBDD"), dp(1)
            )
            addView(statusRow)
            addView(UiKit.statusPills(ctx).also { UiKit.topMargin(ctx, it, 14) })
        }
        UiKit.topMargin(this, statusCard, 8)

        // ---- charts ----
        barChart = WeeklyBarChartView(this)
        trendChart = TrendLineChartView(this)
        heatmap = HeatmapView(this)
        totalPill = UiKit.pill(ctx, "0 total blocked", UiKit.BLUE_DARK, UiKit.BLUE_SOFT)
        summary = UiKit.text(ctx, "", 13f, UiKit.INK)

        val barCard = chartCard("Blocked this week", "Websites & trackers kept out", totalPill, barChart, 220)
        val trendCard = chartCard("Trend", "Daily activity this week", null, trendChart, 200)
        val heatmapCard = chartCard("When did it happen?", "Time slots vs. weekly cadence", null, heatmap, 340)
        heatmapCard.addView(UiKit.infoBox(ctx, "↗", summary).also { UiKit.topMargin(ctx, it, 12) })

        // ---- mutual visibility + button ----
        val mutualTexts = UiKit.vertical(this).apply {
            addView(UiKit.text(ctx, "Mutual Visibility", 14f, UiKit.INK, bold = true))
            addView(UiKit.text(ctx, "Zero hidden keystrokes or camera access", 12f, UiKit.MUTED))
        }
        val mutual = UiKit.infoBox(ctx, "👁", mutualTexts)
        UiKit.topMargin(this, mutual, 16)

        val seeButton = UiKit.button(this, "What does this app see?", filled = false) {
            Nav.open(ctx, Tab.SHARED)
        }

        val column = UiKit.vertical(this).apply {
            setPadding(dp(16), dp(8), dp(16), dp(24))
            addView(statusCard)
            addView(barCard)
            addView(trendCard)
            addView(heatmapCard)
            addView(mutual)
            addView(seeButton)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(UiKit.BG))
            addView(UiKit.topBar(ctx, "Shield Dashboard"))
            addView(ScrollView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(UiKit.MATCH, 0, 1f)
                addView(column)
            })
            addView(Nav.bar(ctx, Tab.SHIELD))
        }
        UiKit.padForSystemBars(root)
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        // Load (or reload) the numbers every time the screen comes back.
        scope.launch {
            val data = withContext(Dispatchers.IO) { repository.getWeeklyChartData() }
            showData(data)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun showData(data: WeeklyChartData) {
        barChart.setData(data.dailyCounts)
        trendChart.setData(data.dailyCounts)
        heatmap.setData(data.heatmap)
        totalPill.text = "${data.dailyCounts.sum()} total blocked"
        summary.text = summarize(data.heatmap)
    }

    /** "Most events happen on Saturday evenings." built from the 28 heatmap numbers. */
    private fun summarize(h: List<Int>): String {
        val max = h.maxOrNull() ?: 0
        if (max == 0) return "No blocked events this week."
        val i = h.indexOf(max)
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val blocks = listOf("nights", "mornings", "afternoons", "evenings")
        return "Most events happen on ${days[i / 4]} ${blocks[i % 4]}."
    }

    private fun chartCard(
        title: String, subtitle: String, badge: View?, chart: View, heightDp: Int
    ): LinearLayout {
        val ctx = this
        val card = UiKit.card(ctx)
        val titles = UiKit.vertical(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f)
            addView(UiKit.text(ctx, title, 17f, UiKit.INK, bold = true))
            addView(UiKit.text(ctx, subtitle, 12f, UiKit.MUTED))
        }
        card.addView(LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(titles)
            if (badge != null) addView(badge)
        })
        chart.layoutParams = LinearLayout.LayoutParams(UiKit.MATCH, UiKit.dp(ctx, heightDp))
            .apply { topMargin = UiKit.dp(ctx, 12) }
        card.addView(chart)
        UiKit.topMargin(ctx, card, 16)
        return card
    }
}