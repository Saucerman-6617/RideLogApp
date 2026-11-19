package com.example.cyclingcheckin.data.repo

import com.example.cyclingcheckin.domain.model.DayStat
import com.example.cyclingcheckin.domain.model.PeriodStat
import com.example.cyclingcheckin.domain.model.Record
import com.example.cyclingcheckin.domain.model.StatsMode
import com.example.cyclingcheckin.domain.model.Validators
import com.example.cyclingcheckin.util.TimeUtils
import java.time.LocalDate
import java.time.YearMonth

/** 统计页一次性快照。 */
data class StatsSnapshot(
    val mode: StatsMode,
    val anchor: LocalDate,
    val rangeStart: LocalDate,
    val rangeEnd: LocalDate,
    val label: String,
    val summary: PeriodStat,
    val days: List<DayStat>,
    val records: List<Record>,
    val streak: Int,
    /** 当前周期 vs 上一周期的总里程变化百分比。null = 上一周期无数据。 */
    val periodOverPeriodPercent: Double?
)

class StatsRepository(
    private val repository: RecordRepository
) {

    fun compute(allRecords: List<Record>, mode: StatsMode, anchor: LocalDate): StatsSnapshot {
        val (start, end) = periodRange(mode, anchor)
        val periodRecords = allRecords
            .filter { !it.date.isBefore(start) && !it.date.isAfter(end) }
            .sortedWith(compareByDescending<Record> { it.date }.thenByDescending { it.time })

        val grouped = periodRecords.groupBy { it.date }
        val days = buildList {
            if (mode == StatsMode.DAY) {
                add(dayStat(anchor, grouped[anchor].orEmpty()))
            } else {
                var cursor = start
                while (!cursor.isAfter(end)) {
                    add(dayStat(cursor, grouped[cursor].orEmpty()))
                    cursor = cursor.plusDays(1)
                }
            }
        }

        val total = Validators.roundDistance(periodRecords.sumOf { it.distance })
        val count = periodRecords.size
        val dayCount = when (mode) {
            StatsMode.DAY -> 1
            StatsMode.WEEK -> 7
            StatsMode.MONTH -> end.dayOfMonth
        }
        val maxDay = if (mode == StatsMode.DAY) null else days.filter { it.count > 0 }.maxByOrNull { it.distance }
        val maxRecord = periodRecords.maxByOrNull { it.distance }
        val label = when (mode) {
            StatsMode.DAY -> "${TimeUtils.dayLabel(anchor)} ${TimeUtils.weekdayLong(anchor)}"
            StatsMode.WEEK -> TimeUtils.weekLabel(start)
            StatsMode.MONTH -> TimeUtils.monthTitle(YearMonth.from(anchor))
        }

        val summary = PeriodStat(
            label = label,
            count = count,
            distance = total,
            avgPerDay = Validators.roundDistance(if (dayCount == 0) 0.0 else total / dayCount),
            maxDay = maxDay,
            maxRecord = maxRecord
        )

        return StatsSnapshot(
            mode = mode,
            anchor = anchor,
            rangeStart = start,
            rangeEnd = end,
            label = label,
            summary = summary,
            days = days,
            records = periodRecords,
            streak = TimeUtils.streakDays(allRecords.map { it.date }.toSet()),
            periodOverPeriodPercent = periodOverPeriodPercent(allRecords, mode, anchor)
        )
    }

    /**
     * 当前周期 vs 上一周期的总里程变化百分比。
     * - DAY  → 前一天
     * - WEEK → 上一周
     * - MONTH → 上一月
     * 上一周期无数据时返回 null。
     */
    fun periodOverPeriodPercent(
        allRecords: List<Record>,
        mode: StatsMode,
        anchor: LocalDate
    ): Double? {
        val (curStart, curEnd) = periodRange(mode, anchor)
        val curTotal = allRecords.filter { !it.date.isBefore(curStart) && !it.date.isAfter(curEnd) }
            .sumOf { it.distance }
        val prevAnchor = when (mode) {
            StatsMode.DAY -> anchor.minusDays(1)
            StatsMode.WEEK -> TimeUtils.startOfWeek(anchor).minusDays(7)
            StatsMode.MONTH -> YearMonth.from(anchor).minusMonths(1).atEndOfMonth()
        }
        val (prevStart, prevEnd) = periodRange(mode, prevAnchor)
        val prevTotal = allRecords.filter { !it.date.isBefore(prevStart) && !it.date.isAfter(prevEnd) }
            .sumOf { it.distance }
        if (prevTotal <= 0.0) return null
        return (curTotal - prevTotal) / prevTotal * 100.0
    }

    private fun periodRange(mode: StatsMode, anchor: LocalDate): Pair<LocalDate, LocalDate> = when (mode) {
        StatsMode.DAY -> anchor to anchor
        StatsMode.WEEK -> TimeUtils.startOfWeek(anchor) to TimeUtils.startOfWeek(anchor).plusDays(6)
        StatsMode.MONTH -> TimeUtils.startOfMonth(anchor) to TimeUtils.endOfMonth(anchor)
    }

    private fun dayStat(date: LocalDate, records: List<Record>): DayStat = DayStat(
        date = date,
        count = records.size,
        distance = Validators.roundDistance(records.sumOf { it.distance })
    )

    fun observeAll(): kotlinx.coroutines.flow.Flow<List<Record>> = repository.observeAll()
}
