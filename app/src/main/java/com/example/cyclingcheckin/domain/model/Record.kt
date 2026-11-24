package com.example.cyclingcheckin.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** 一条骑行记录（领域模型，日期与时间分离存储）。 */
data class Record(
    val id: Long,
    val date: LocalDate,
    val time: LocalTime,
    val distance: Double,
    val note: String,
    val routeStart: String = "",
    val routeEnd: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val dirty: Boolean
) {
    val dateTime: LocalDateTime get() = LocalDateTime.of(date, time)
}
