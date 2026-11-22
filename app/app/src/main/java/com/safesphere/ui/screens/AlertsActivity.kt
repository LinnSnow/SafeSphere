package com.safesphere.ui.screens

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import com.safesphere.data.AppDatabase
import com.safesphere.data.ChartDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Bell icon: counts only (today, this week, busiest time) plus the protection history. Never content. */
class AlertsActivity : Activity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val repository by lazy {
        ChartDataRepository(AppDatabase.getInstance(this).blockedEventDao())
    }
    private val isParent get() = AppPrefs.getRole(this) == AppPrefs.ROLE_PARENT
    private lateinit var host: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        fun dp(v: Int) = UiKit.dp(ctx, v)

        val back = UiKit.roundIcon(ctx, "←", 40, "#E8EEFD", UiKit.INK, 18f).apply {
            setOnClickListener { finish() }
        }
        val titles = UiKit.vertical(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply { leftMargin = dp(12) }
            addView(UiKit.text(ctx, "Alerts", 20f, UiKit.INK, bold = true))
            addView(UiKit.text(ctx, "Counts only, never content", 12f, UiKit.MUTED))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
            addView(back)
            addView(titles)
        }
        host = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(UiKit.MATCH, 0, 1f) }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(UiKit.BG))
            addView(header)
            addView(host)
        }
        UiKit.padForSystemBars(root)
        setContentView(root)
        render(null, null, emptyList())
    }

    override fun onResume() {
        super.onResume()
        scope.launch { load() }
    }

    private suspend fun load() {
        var daily: List<Int>? = null
        var heat: List<Int>? = null
        var history: List<ProtectionLog.Entry> = emptyList()
        if (isParent) {
            when (val r = Session.loadParent(this)) {
                is ParentResult.Ok -> {
                    daily = r.weekly?.dailyCounts
                    heat = r.weekly?.heatmap
                    // Exact history from the server; if it can't be reached, what this phone noticed.
                    history = Session.loadProtectionLog(this)
                        ?: ProtectionLog.recent(this, 10, ProtectionLog.WATCH)
                }
                ParentResult.Unpaired -> {
                    Session.leaveToWelcome(this)
                    return
                }
                ParentResult.Offline -> {
                    history = ProtectionLog.recent(this, 10, ProtectionLog.WATCH)
                }
            }
        } else {
            val d = withContext(Dispatchers.IO) { repository.getWeeklyChartData() }
            daily = d.dailyCounts
            heat = d.heatmap
            history = ProtectionLog.recent(this, 10, ProtectionLog.LOCAL)
        }
        render(daily, heat, history)
    }

    private fun render(daily: List<Int>?, heat: List<Int>?, history: List<ProtectionLog.Entry>) {
        val ctx = this
        fun dp(v: Int) = UiKit.dp(ctx, v)

        val dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)   // Sunday = 1
        val todayIndex = (dow + 5) % 7                                // Monday = 0 ... Sunday = 6
        val today = daily?.getOrNull(todayIndex) ?: 0
        val week = daily?.sum() ?: 0

        // ---- today ----
        val todayCard = UiKit.card(this).apply {
            addView(UiKit.text(ctx, "Blocked today", 13f, UiKit.MUTED, bold = true))
            addView(UiKit.text(ctx, "$today", 44f, UiKit.INK, bold = true))
            addView(UiKit.text(ctx, "$week blocked this week", 13f, UiKit.MUTED))
        }
        UiKit.topMargin(this, todayCard, 8)

        // ---- busiest time ----
        val busiest = UiKit.card(this).apply {
            addView(UiKit.text(ctx, "Busiest time", 17f, UiKit.INK, bold = true))
            addView(UiKit.text(ctx, summarize(heat), 14f, UiKit.MUTED).also { UiKit.topMargin(ctx, it, 6) })
        }
        UiKit.topMargin(this, busiest, 16)

        val column = UiKit.vertical(this).apply {
            setPadding(dp(16), dp(8), dp(16), dp(24))
            addView(todayCard)
            addView(busiest)
        }

        // ---- protection history ----
        val historyCard = UiKit.card(this).apply {
            addView(UiKit.text(
                ctx, if (isParent) "Your teen's protection history" else "Protection history",
                17f, UiKit.INK, bold = true
            ))
            if (history.isEmpty()) {
                addView(UiKit.text(
                    ctx, "No changes yet. Protection has stayed on.", 14f, UiKit.MUTED
                ).also { UiKit.topMargin(ctx, it, 6) })
            } else {
                history.forEach { addView(historyRow(it).also { r -> UiKit.topMargin(ctx, r, 12) }) }
            }
        }
        UiKit.topMargin(this, historyCard, 16)
        column.addView(historyCard)

        val note = UiKit.infoBox(
            ctx, "🔒",
            UiKit.text(
                ctx, "SafeSphere never saves what was blocked. It only counts that something was, and when.",
                12f, UiKit.INK
            )
        )
        UiKit.topMargin(this, note, 16)
        column.addView(note)

        host.removeAllViews()
        host.addView(column)
    }

    private fun day(ms: Long): String =
        SimpleDateFormat("EEE, h:mm a", Locale.getDefault()).format(Date(ms))

    private fun clock(ms: Long): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ms))

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
                addView(UiKit.text(ctx, day(e.at), 12f, UiKit.MUTED))
            })
        }
    }

    private fun summarize(h: List<Int>?): String {
        val max = h?.maxOrNull() ?: 0
        if (h == null || max == 0) return "No blocked events this week."
        val i = h.indexOf(max)
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val blocks = listOf("nights", "mornings", "afternoons", "evenings")
        return "Most attempts happen on ${days[i / 4]} ${blocks[i % 4]}."
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}