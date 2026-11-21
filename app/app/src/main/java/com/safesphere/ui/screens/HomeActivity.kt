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
    private lateinit var shield: AnimatedShieldView
    private lateinit var statusCard: LinearLayout
    private lateinit var statusTitle: TextView
    private lateinit var statusSub: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        fun dp(v: Int) = UiKit.dp(ctx, v)

        // ---- status card ----
        shield = AnimatedShieldView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(64), dp(64))
            setState(ShieldState.ON)
        }
        val gear = UiKit.roundIcon(ctx, "⚙", 40, UiKit.BLUE_SOFT, UiKit.BLUE_DARK, 20f).apply {
            setOnClickListener {
                startActivity(Intent(ctx, ProtectionSettingsActivity::class.java))
            }
        }
        statusTitle = UiKit.text(ctx, "Protection is ON", 20f, UiKit.INK, bold = true)
        // TODO: real "last checked" time from the heartbeat (GET /api/status/{device_id})
        statusSub = UiKit.text(ctx, "Last checked 2 min ago", 12f, UiKit.MUTED)
        val statusTexts = UiKit.vertical(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply { leftMargin = dp(12) }
            addView(statusTitle)
            addView(statusSub)
        }
        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(shield)
            addView(statusTexts)
            addView(gear)
        }
        statusCard = UiKit.card(this).apply {
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

        val barCard = chartCard("Blocked this week", "Websites & trackers kept out", totalPill, barChart, 230)
        val trendCard = chartCard("Trend", "Daily activity this week", null, trendChart, 200)
        val heatmapCard = chartCard("When did it happen?", "Time slots vs. weekly cadence", legend(), heatmap, 190)
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
            addView(UiKit.topBar(ctx, "Shield Dashboard") { showDemoMenu() })
            addView(ScrollView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(UiKit.MATCH, 0, 1f)
                addView(column)
            })
            addView(Nav.bar(ctx, Tab.SHIELD))
        }
        UiKit.padForSystemBars(root)
        setContentView(root)
        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
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

    /** Green card when protection is on; red card when it was turned off or paused (from Protection settings). */
    private fun updateStatus() {
        val on = AppPrefs.isProtectionOn(this)
        val paused = AppPrefs.isPaused(this)
        val ctx = this
        val dp = { v: Int -> UiKit.dp(ctx, v) }
        when {
            !on -> {
                statusTitle.text = "Protection is OFF"
                statusSub.text = "Turned off with the parent PIN"
                shield.setState(ShieldState.OFF)
            }
            paused -> {
                statusTitle.text = "Protection is paused"
                statusSub.text = "It resumes automatically"
                shield.setState(ShieldState.OFF)
            }
            else -> {
                statusTitle.text = "Protection is ON"
                statusSub.text = "Last checked 2 min ago"
                shield.setState(ShieldState.ON)
            }
        }
        val good = on && !paused
        statusCard.background = UiKit.rounded(
            Color.parseColor(if (good) "#E6F8EE" else "#FFEDEB"),
            dp(16).toFloat(),
            Color.parseColor(if (good) "#C6EBD6" else "#F5B8B2"),
            dp(1)
        )
    }

    private fun showData(data: WeeklyChartData) {
        barChart.setData(data.dailyCounts)
        trendChart.setData(data.dailyCounts)
        heatmap.setData(data.heatmap)
        totalPill.text = "${data.dailyCounts.sum()} total blocked"
        summary.text = summarize(data.heatmap)
    }

    /** "Most attempts happen on Saturday evenings." built from the 28 heatmap numbers. */
    private fun summarize(h: List<Int>): String {
        val max = h.maxOrNull() ?: 0
        if (max == 0) return "No blocked events this week."
        val i = h.indexOf(max)
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val blocks = listOf("nights", "mornings", "afternoons", "evenings")
        return "Most attempts happen on ${days[i / 4]} ${blocks[i % 4]}."
    }

    /** Demo helper (long-press the "SafeSphere" title). TODO: remove before the final version. */
    private fun showDemoMenu() {
        val items = arrayOf("Preview: Text warning", "Preview: Site blocked", "Reset setup (start from Welcome)")
        android.app.AlertDialog.Builder(this)
            .setTitle("Demo tools")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(this, TextWarningActivity::class.java))
                    1 -> startActivity(Intent(this, SiteBlockedActivity::class.java))
                    2 -> {
                        AppPrefs.setSetupDone(this, false)
                        AppPrefs.setProtectionOn(this, true)
                        startActivity(
                            Intent(this, WelcomeActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        )
                    }
                }
            }
            .show()
    }

    /** Three little squares: few -> many events. */
    private fun legend(): LinearLayout {
        val ctx = this
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            listOf("#FDE047", "#FB923C", "#991B1B").forEach { c ->
                addView(View(ctx).apply {
                    background = UiKit.rounded(Color.parseColor(c), UiKit.dp(ctx, 3).toFloat())
                    layoutParams = LinearLayout.LayoutParams(UiKit.dp(ctx, 12), UiKit.dp(ctx, 12))
                        .apply { leftMargin = UiKit.dp(ctx, 4) }
                })
            }
        }
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