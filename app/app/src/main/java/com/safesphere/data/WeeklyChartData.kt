package com.safesphere.data

data class WeeklyChartData(
    val dailyCounts: List<Int>,   // 7 ints, Monday se Sunday tak
    val heatmap: List<Int>        // 28 ints, index = day * 4 + timeBlock
)