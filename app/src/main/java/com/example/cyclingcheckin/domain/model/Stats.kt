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
    /** 本周期内单日总里程最高的那天（周/月视图显示，日视图为 null）。 */
    val maxDay: DayStat?,
    /** 本周期内单次骑行距离最远的那条记录（日视图显示其时间）。 */
    val maxRecord: Record?
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
