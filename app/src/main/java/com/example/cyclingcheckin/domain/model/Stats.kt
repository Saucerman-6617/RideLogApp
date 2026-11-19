package com.example.cyclingcheckin.domain.model

import java.time.LocalDate

/** 单日聚合统计。 */
data class DayStat(
    val date: LocalDate,
    val count: Int,
    val distance: Double
)

/** 一个统计周期（日/周/月）的汇总。 */
data class PeriodStat(
    val label: String,
    val count: Int,
    val distance: Double,
    val avgPerDay: Double,
    val maxDay: DayStat?
)

enum class StatsMode {
    DAY,
    WEEK,
    MONTH
}

/** 导入结果。 */
data class ImportResult(
    val imported: Int,
    val skipped: Int
)
