package com.safesphere.ui.screens

import android.app.Activity
import android.app.AlertDialog
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Family tab.
 *  Parent phone: real status, weekly numbers and protection log from the backend, refreshed every 10 seconds.
 *                When the teen's protection turns off or is paused (or comes back) a pop-up appears.
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
    private var offlineShown = false
    private var lastSeenMinutes = 0
    private var lastEvent: ProtectionLog.Entry? = null
    private var history: List<ProtectionLog.Entry> = emptyList()
    private var daily: List<Int>? = null
    private var heat: List<Int>? = null
    private var pollJob: Job? = null
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
        if (isParent) ParentWatcher.schedule(this)   // background check while the app is closed
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                if (isParent) loadFromBackend() else loadLocal()
                delay(10_000)
            }
        }
    }

    override fun onPause() {
        pollJob?.cancel()
        super.onPause()
    }

    private suspend fun loadFromBackend() {
        when (val r = Session.loadParent(this)) {
            is ParentResult.Ok -> {
                offlineShown = false
                val s = r.status
                val newConnected = s?.protectionActive ?: false
                val newSeen = s?.lastSeenMinutesAgo ?: 0
                val newDaily = r.weekly?.dailyCounts
                val newHeat = r.weekly?.heatmap
                // Exact history from the server; if it can't be loaded, keep what we had (or what this phone noticed).
                val newHistory = Session.loadProtectionLog(this)
                    ?: history.ifEmpty { ProtectionLog.recent(this, 10, ProtectionLog.WATCH) }
                val newEvent = newHistory.firstOrNull()
                val changed = !loaded || newConnected != connected || newSeen != lastSeenMinutes ||
                        newDaily != daily || newHeat != heat || newEvent != lastEvent ||
                        newHistory != history
                connected = newConnected
                lastSeenMinutes = newSeen
                daily = newDaily
                heat = newHeat
                history = newHistory
                lastEvent = newEvent
                loaded = true
                if (s != null) announce(ParentWatcher.apply(this, s.protectionActive))
                if (changed) render()
            }
            ParentResult.Unpaired -> Session.leaveToWelcome(this)
            ParentResult.Offline -> {
                if (!offlineShown) {
                    offlineShown = true
                    Toast.makeText(
                        this, "Couldn't reach the server. Showing the last numbers.", Toast.LENGTH_LONG
                    ).show()
                }
                if (!loaded) {
                    loaded = true
                    render()
                }
            }
        }
    }

    private suspend fun loadLocal() {
        val newConnected = AppPrefs.isProtectionActive(this)
        val d = withContext(Dispatchers.IO) { repository.getWeeklyChartData() }
        val newHistory = ProtectionLog.recent(this, 10, ProtectionLog.LOCAL)
        val changed = !loaded || newConnected != connected || d.dailyCounts != daily || d.heatmap != heat ||
                newHistory != history
        connected = newConnected
        daily = d.dailyCounts
        heat = d.heatmap
        history = newHistory
        loaded = true
        if (changed) render()
    }

    /** Pop-up when the parent's phone notices a change. */
    private fun announce(kind: String?) {
        if (kind == null || isFinishing) return
        val now = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        val detail = lastEvent?.let { ProtectionLog.describe(it) }
        if (kind == "off") {
            AlertDialog.Builder(this)
                .setTitle("Protection is off")
                .setMessage(detail ?: "Your teen's phone reports that protection was turned off or paused. Noticed at $now.")
                .setPositiveButton("OK", null)
                .show()
        } else {
            Toast.makeText(this, detail ?: "Protection is back on (noticed at $now)", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /** Rebuilds the screen but keeps the scroll position. */
    private fun render() {
        val y = host.scrollY
        host.removeAllViews()
        host.addView(buildColumn())
        host.post { host.scrollTo(0, y) }
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
        column.addView(historyCard())

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

    private fun clock(ms: Long): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ms))

    private fun dayTime(ms: Long): String =
        SimpleDateFormat("EEE, h:mm a", Locale.getDefault()).format(Date(ms))

    /** "Protection history": when protection was turned off, paused (until when) and turned back on. */
    private fun historyCard(): LinearLayout {
        val ctx = this
        val card = UiKit.card(ctx)
        card.addView(UiKit.text(
            ctx, if (isParent) "Your teen's protection history" else "Protection history",
            17f, UiKit.INK, bold = true
        ))
        card.addView(UiKit.text(ctx, "When protection was turned off, paused or back on", 12f, UiKit.MUTED))
        val shown = history.take(5)
        if (shown.isEmpty()) {
            card.addView(UiKit.text(
                ctx, "No changes yet. Protection has stayed on.", 14f, UiKit.MUTED
            ).also { UiKit.topMargin(ctx, it, 10) })
        } else {
            shown.forEach { card.addView(historyRow(it).also { r -> UiKit.topMargin(ctx, r, 12) }) }
            card.addView(UiKit.button(ctx, "See full history", filled = false, colorHex = UiKit.BLUE_DARK) {
                startActivity(Intent(ctx, AlertsActivity::class.java))
            }.also { UiKit.topMargin(ctx, it, 12) })
        }
        UiKit.topMargin(ctx, card, 16)
        return card
    }

    private fun historyRow(e: ProtectionLog.Entry): LinearLayout {
        val ctx = this
        val icon: String
        val bg: String
        val fg: String
        val title: String
        when (e.kind) {
            "off" -> { icon = "power"; bg = Pal.RED_SOFT; fg = Pal.RED; title = "Protection turned off" }
            "paused" -> {
                icon = "clock"; bg = "#FEF3C7"; fg = "#B45309"
                title = if (e.until > 0L) "Paused until ${clock(e.until)}" else "Protection paused"
            }
            "removed" -> { icon = "alert"; bg = Pal.RED_SOFT; fg = Pal.RED; title = "SafeSphere was removed" }
            "on" -> { icon = "shield-check"; bg = Pal.GREEN_SOFT; fg = Pal.GREEN_DARK; title = "Protection turned on" }
            else -> { icon = "shield-check"; bg = Pal.GREEN_SOFT; fg = Pal.GREEN_DARK; title = "Protection resumed" }
        }
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(Icons.circle(ctx, icon, 36, bg, fg))
            addView(UiKit.vertical(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply { leftMargin = UiKit.dp(ctx, 12) }
                addView(UiKit.text(ctx, title, 14f, UiKit.INK, bold = true))
                addView(UiKit.text(ctx, dayTime(e.at), 12f, UiKit.MUTED))
            })
        }
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

        val event = lastEvent
        val bodyText = when {
            !isParent -> "Protection is turned off or paused on this phone."
            lastSeenMinutes >= 60 ->
                "No signal from the teen's device for ${lastSeenMinutes / 60} hours. Screen time and network shields are unresponsive."
            event != null && event.kind != "on" -> ProtectionLog.describe(event) + "."
            else ->
                "The teen's device checked in ${ago(lastSeenMinutes)} and reports that protection is off or paused."
        }
        val body = UiKit.text(ctx, bodyText, 13f, UiKit.INK)
        UiKit.topMargin(ctx, body, 6)
        val check = UiKit.button(this, if (isParent) "See history" else "Check settings", filled = true, colorHex = UiKit.BLUE_DARK) {
            if (isParent) startActivity(Intent(ctx, AlertsActivity::class.java)) else openSettings()
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