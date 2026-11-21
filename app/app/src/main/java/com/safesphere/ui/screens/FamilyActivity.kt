package com.safesphere.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import com.safesphere.data.AppDatabase
import com.safesphere.data.ChartDataRepository
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
 * Family tab.
 *  Parent phone: real status + weekly numbers from the backend (status() and weekly()).
 *  Teen phone:   this phone's own state and local numbers.
 * Connected -> green "Protection is ON". Otherwise red "Protection may be off", charts greyed out.
 */
class FamilyActivity : Activity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val repository by lazy {
        ChartDataRepository(AppDatabase.getInstance(this).blockedEventDao())
    }

    private val isParent get() = AppPrefs.getRole(this) == AppPrefs.ROLE_PARENT

    private var connected = true
    private var loaded = false
    private var lastSeenMinutes = 0
    private var daily: List<Int>? = null
    private var heat: List<Int>? = null
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
            // Long-press the title = demo switch between the green and red look.
            addView(UiKit.topBar(ctx, "Shield Dashboard") {
                connected = !connected
                loaded = true
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
            if (isParent) loadFromBackend() else loadLocal()
        }
    }

    private suspend fun loadFromBackend() {
        when (val r = Session.loadParent(this)) {
            is ParentResult.Ok -> {
                connected = r.status?.protectionActive ?: false
                lastSeenMinutes = r.status?.lastSeenMinutesAgo ?: 0
                daily = r.weekly?.dailyCounts
                heat = r.weekly?.heatmap
                loaded = true
                render()
            }
            ParentResult.Unpaired -> Session.leaveToWelcome(this)
            ParentResult.Offline -> {
                Toast.makeText(
                    this, "Couldn't reach the server. Showing the last numbers.", Toast.LENGTH_LONG
                ).show()
                loaded = true
                render()
            }
        }
    }

    private suspend fun loadLocal() {
        connected = AppPrefs.isProtectionActive(this)
        val d = withContext(Dispatchers.IO) { repository.getWeeklyChartData() }
        daily = d.dailyCounts
        heat = d.heatmap
        loaded = true
        render()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun render() {
        host.removeAllViews()
        host.addView(buildColumn())
    }

    private fun openSettings() {
        val target = if (isParent) ParentSettingsActivity::class.java else ProtectionSettingsActivity::class.java
        startActivity(Intent(this, target))
    }

    private fun ago(minutes: Int): String = when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        else -> "${minutes / 60} h ago"
    }

    private fun buildColumn(): LinearLayout {
        val ctx = this
        val column = UiKit.vertical(this).apply {
            setPadding(UiKit.dp(ctx, 16), UiKit.dp(ctx, 8), UiKit.dp(ctx, 16), UiKit.dp(ctx, 24))
        }

        if (isParent && !loaded) {
            column.addView(UiKit.card(this).apply {
                addView(UiKit.text(ctx, "Connecting to your teen's device…", 16f, UiKit.INK, bold = true))
                addView(UiKit.text(
                    ctx, "The server can take up to a minute to wake up the first time.", 13f, UiKit.MUTED
                ).also { UiKit.topMargin(ctx, it, 6) })
            }.also { UiKit.topMargin(ctx, it, 12) })
            return column
        }

        column.addView(if (connected) connectedSection() else disconnectedSection())

        val bar = WeeklyBarChartView(this)
        val heatmap = HeatmapView(this)
        daily?.let { bar.setData(it) }
        heat?.let { heatmap.setData(it) }
        val barCard = chartCard("Blocked this week", "Past 7 days", bar, 230)
        val heatCard = chartCard("When did it happen?", "Time slots vs. weekly cadence", heatmap, 190)
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
            setOnClickListener { openSettings() }
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
            addView(UiKit.text(
                ctx,
                if (isParent) "Teen's device checked in ${ago(lastSeenMinutes)}" else "Protection is active on this phone",
                12f, UiKit.MUTED
            ))
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(shield)
            addView(texts)
        }
        val card = UiKit.card(this).apply {
            background = UiKit.rounded(
                Color.parseColor("#E6F8EE"), dp(16).toFloat(), Color.parseColor("#C6EBD6"), dp(1)
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
        val badge = Icons.pill(ctx, "alert", "Disconnected", "#FFFFFF", "#BA1A1A", 11f).apply {
            (layoutParams as LinearLayout.LayoutParams).leftMargin = dp(12)
        }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(alertShield)
            addView(badge)
        }
        val title = UiKit.text(ctx, "Protection may be off", 22f, UiKit.RED, bold = true)
        UiKit.topMargin(ctx, title, 12)

        val bodyText = when {
            !isParent -> "Protection is turned off or paused on this phone."
            lastSeenMinutes >= 60 ->
                "No signal from the teen's device for ${lastSeenMinutes / 60} hours. Screen time and network shields are unresponsive."
            else ->
                "The teen's device checked in ${ago(lastSeenMinutes)} and reports that protection is off or paused."
        }
        val body = UiKit.text(ctx, bodyText, 13f, UiKit.INK)
        UiKit.topMargin(ctx, body, 6)
        val check = UiKit.button(this, "Check settings", filled = true, colorHex = UiKit.BLUE_DARK) {
            openSettings()
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
            addView(smallCard("battery", "#B45309", "Battery / Low Power", "May restrict sync", true))
            addView(smallCard("wifi-off", UiKit.MUTED, "Network Offline", "Airplane mode on?", false))
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

    private fun smallCard(iconName: String, colorHex: String, title: String, sub: String, first: Boolean): LinearLayout {
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
            addView(Icons.icon(ctx, iconName, 20, colorHex))
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