package com.safesphere

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.safesphere.ui.charts.HeatmapView
import com.safesphere.ui.charts.TrendLineChartView
import com.safesphere.ui.charts.WeeklyBarChartView
import android.view.Gravity
import com.safesphere.ui.shield.AnimatedShieldView
import com.safesphere.ui.shield.ShieldState

/**
 * Temporary demo screen: shows both charts with fake data.
 * Everything is built in code, so no XML layout is needed.
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val d = resources.displayMetrics.density
        fun px(v: Int) = (v * d).toInt()

        fun title(text: String, size: Float, bold: Boolean = false) = TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(Color.parseColor("#212121"))
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, px(12), 0, px(8))
        }

        // ---- fake data (same numbers as the team doc) ----
        val dailyCounts = listOf(3, 5, 0, 2, 8, 12, 1)
        val heatmap = listOf(
            0, 0, 1, 2,   // Mon = 3
            0, 0, 2, 3,   // Tue = 5
            0, 0, 0, 0,   // Wed = 0
            0, 0, 0, 2,   // Thu = 2
            1, 0, 2, 5,   // Fri = 8
            0, 1, 4, 7,   // Sat = 12
            0, 0, 0, 1    // Sun = 1
        )

        val barChart = WeeklyBarChartView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(260))
            setData(dailyCounts)
        }
        val trendChart = TrendLineChartView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(260))
            setData(dailyCounts)
        }
        val heatmapView = HeatmapView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(360))
            setData(heatmap)
        }

        val shield = AnimatedShieldView(this).apply {
            layoutParams = LinearLayout.LayoutParams(px(160), px(160)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            setOnClickListener {
                setState(
                    when (state) {
                        ShieldState.ON -> ShieldState.ALERT
                        ShieldState.ALERT -> ShieldState.OFF
                        ShieldState.OFF -> ShieldState.ON
                    }
                )
            }
        }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(16), px(48), px(16), px(24))   // top padding keeps clear of status bar
            addView(title("SafeSphere", 28f, bold = true))
            addView(shield)
            addView(title("Blocked events this week", 16f))
            addView(barChart)
            addView(title("Trend", 16f))
            addView(trendChart)
            addView(title("When did it happen?", 16f))
            addView(heatmapView)
        }

        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.WHITE)
            addView(column)
        })
    }
}