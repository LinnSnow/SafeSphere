package com.safesphere.data

import java.util.Calendar

class ChartDataRepository(
    private val dao: BlockedEventDao
) {

    suspend fun getWeeklyChartData(): WeeklyChartData {
        val start = getWeekStartMillis()
        val end = Calendar.getInstance().apply {
            timeInMillis = start
            add(Calendar.DAY_OF_YEAR, 7)
        }.timeInMillis

        // 7 din, shuru mein sab 0 (zero-fill)
        val dailyCounts = MutableList(7) { 0 }
        for (row in dao.getDailyCounts(start, end)) {
            val index = (row.day + 6) % 7   // Sunday-first ko Monday-first banata hai
            dailyCounts[index] = row.count
        }

        // 28 blocks (7 din x 4 time blocks), shuru mein sab 0
        val heatmap = MutableList(28) { 0 }
        for (row in dao.getHeatmapCounts(start, end)) {
            val index = ((row.day + 6) % 7) * 4 + row.block
            if (index in 0..27) heatmap[index] = row.count
        }

        return WeeklyChartData(dailyCounts, heatmap)
    }

    // Is hafte ka Monday 00:00 (local time), epoch millis mein
    private fun getWeekStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val daysSinceMonday = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        cal.add(Calendar.DAY_OF_YEAR, -daysSinceMonday)
        return cal.timeInMillis
    }
}