package com.example.cyclingcheckin.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeUtils {
    val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private val WEEKDAY_SHORT = listOf("一", "二", "三", "四", "五", "六", "日")

    fun today(): LocalDate = LocalDate.now()

    fun now(): LocalDateTime = LocalDateTime.now()

    fun parseDate(value: String): LocalDate? =
        runCatching { LocalDate.parse(value, DATE_FORMAT) }.getOrNull()

    fun parseTime(value: String): LocalTime? =
        runCatching { LocalTime.parse(value, TIME_FORMAT) }.getOrNull()

    fun formatDate(date: LocalDate): String = date.format(DATE_FORMAT)

    fun formatTime(time: LocalTime): String = time.format(TIME_FORMAT)

    fun formatDistance(distance: Double): String = String.format(Locale.US, "%.1f", distance)

    fun monthTitle(month: YearMonth): String = "${month.year}年${month.monthValue}月"

    fun dayLabel(date: LocalDate): String = "${date.monthValue}月${date.dayOfMonth}日"

    fun fullDateLabel(date: LocalDate): String =
        "${date.year}年${date.monthValue}月${date.dayOfMonth}日"

    fun formatTimestamp(timestamp: Long): String {
        val dateTime = java.time.Instant.ofEpochMilli(timestamp)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDateTime()
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    }

    fun weekdayShort(date: LocalDate): String = WEEKDAY_SHORT[date.dayOfWeek.value - 1]

    fun weekdayLong(date: LocalDate): String = "周${weekdayShort(date)}"

    /** 周视图标题：11月17日 – 11月23日。 */
    fun weekLabel(start: LocalDate): String {
        val end = start.plusDays(6)
        return "${dayLabel(start)} – ${dayLabel(end)}"
    }

    fun startOfWeek(date: LocalDate): LocalDate = date.with(DayOfWeek.MONDAY)

    fun startOfMonth(date: LocalDate): LocalDate = date.withDayOfMonth(1)

    fun endOfMonth(date: LocalDate): LocalDate = date.withDayOfMonth(date.lengthOfMonth())

    /** 生成月历 6×7 网格（周一为第一列），空位为 null。 */
    fun monthGrid(month: YearMonth): List<LocalDate?> {
        val first = month.atDay(1)
        val offset = first.dayOfWeek.value - 1
        val days = month.lengthOfMonth()
        val cells = ArrayList<LocalDate?>(42)
        repeat(offset) { cells.add(null) }
        for (day in 1..days) {
            cells.add(month.atDay(day))
        }
        while (cells.size < 42) {
            cells.add(null)
        }
        return cells
    }

    /** 连续打卡天数：从今天（若今天未打卡则从昨天）向前连续计数。 */
    fun streakDays(dates: Set<LocalDate>, today: LocalDate = LocalDate.now()): Int {
        if (dates.isEmpty()) return 0
        var cursor = if (dates.contains(today)) today else today.minusDays(1)
        var streak = 0
        while (dates.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }
}
