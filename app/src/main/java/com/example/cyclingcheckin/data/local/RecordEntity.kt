package com.example.cyclingcheckin.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "record",
    indices = [Index(value = ["date"], name = "idx_record_date")]
)
data class RecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val time: String,
    val distance: Double,
    val note: String = "",
    val routeStart: String = "",
    val routeEnd: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val dirty: Boolean = true
)

/** Room 聚合查询结果（date 为 yyyy-MM-dd）。 */
data class DayStatRow(
    val date: String,
    val count: Int,
    val distance: Double
)
