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

/**
 * Home screen: status card (animated shield), the three charts, a plain-language
 * summary, and a button to the transparency screen.
 * Chart numbers come ONLY from ChartDataRepository.getWeeklyChartData().
 */
class HomeActivity : Activity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val repository = ChartDataRepository()

    private lateinit var shield: AnimatedShieldView
    private lateinit var statusTitle: TextView
    private lateinit var statusLine: TextView
    private lateinit var barChart: WeeklyBarChartView
    private lateinit var trendChart: TrendLineChartView
    private lateinit var heatmap: HeatmapView
    private lateinit var summary: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        fun dp(v: Int) = UiKit.dp(ctx, v)

        // ---- header: app name + settings gear ----
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(UiKit.text(ctx, "SafeSphere", 28f, bold = true).apply {
                layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f)
            })
            addView(UiKit.text(ctx, "⚙", 26f, UiKit.MUTED).apply {
                setPadding(dp(8), dp(4), dp(8), dp(4))
                // TODO: open ProtectionSettingsActivity (PIN-gated) once it exists
                setOnClickListener {
                    Toast.makeText(ctx, "Settings: coming soon", Toast.LENGTH_SHORT).show()
                }
            })
        }

        // ---- status card ----
        shield = AnimatedShieldView(this).apply { layoutParams = LinearLayout.LayoutParams(dp(72), dp(72)) }
        statusTitle = UiKit.text(ctx, "", 18f, UiKit.GREEN, bold = true)
        statusLine = UiKit.text(ctx, "", 13f, UiKit.MUTED)
        val statusTexts = UiKit.vertical(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply { leftMargin = dp(12) }
            addView(statusTitle)
            addView(statusLine)
        }
        val statusCard = UiKit.card(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(shield)
            addView(statusTexts)
        }
        UiKit.topMargin(this, statusCard, 16)
        // TODO: drive this from the heartbeat (GET /api/status/{device_id}) when Person 4's code is ready
        showStatus(true, "Live status will come from the backend")

        // ---- charts ----
        barChart = WeeklyBarChartView(this)
        trendChart = TrendLineChartView(this)
        heatmap = HeatmapView(this)
        summary = UiKit.text(ctx, "", 14f, UiKit.MUTED).apply { setPadding(0, dp(8), 0, 0) }

        val barCard = chartCard("Blocked this week", barChart, 240)
        val trendCard = chartCard("Trend", trendChart, 220)
        val heatmapCard = chartCard("When did it happen?", heatmap, 340).apply { addView(summary) }

        // ---- button ----
        val seeButton = UiKit.button(this, "What does this app see?", filled = false) {
            startActivity(Intent(ctx, TransparencyActivity::class.java))
        }

        val column = UiKit.vertical(this).apply {
            setPadding(dp(16), dp(48), dp(16), dp(32))
            addView(header)
            addView(statusCard)
            addView(barCard)
            addView(trendCard)
            addView(heatmapCard)
            addView(seeButton)
        }
        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.parseColor(UiKit.BG))
            addView(column)
        })
    }

    override fun onResume() {
        super.onResume()
        // Load (or reload) the numbers every time the screen comes back
        scope.launch {
            val data = repository.getWeeklyChartData()
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
        summary.text = summarize(data.heatmap)
    }

    private fun showStatus(active: Boolean, detail: String) {
        shield.setState(if (active) ShieldState.ON else ShieldState.ALERT)
        statusTitle.text = if (active) "Protection is ON" else "Protection may be off"
        statusTitle.setTextColor(Color.parseColor(if (active) UiKit.GREEN else UiKit.RED))
        statusLine.text = detail
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

    private fun chartCard(title: String, chart: View, heightDp: Int): LinearLayout {
        val card = UiKit.card(this)
        card.addView(UiKit.text(this, title, 16f, bold = true))
        chart.layoutParams = LinearLayout.LayoutParams(UiKit.MATCH, UiKit.dp(this, heightDp))
        card.addView(chart)
        UiKit.topMargin(this, card, 16)
        return card
    }
}