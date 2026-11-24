package com.example.cyclingcheckin.data.remote

import com.example.cyclingcheckin.domain.model.FrequentAddress
import com.example.cyclingcheckin.domain.model.Record
import com.example.cyclingcheckin.util.TimeUtils
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class BackupFile(
    val app: String = BackupCodec.APP_ID,
    val schemaVersion: Int = BackupCodec.SCHEMA_VERSION,
    val exportedAt: String = "",
    val device: String = "",
    val records: List<BackupRecord> = emptyList(),
    val frequentAddresses: List<FrequentAddress> = emptyList()
)

@Serializable
data class BackupRecord(
    val id: Long = 0,
    val date: String,
    val time: String,
    val distance: Double,
    val note: String = "",
    val routeStart: String = "",
    val routeEnd: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

object BackupCodec {
    const val APP_ID = "cyclingcheckin"
    const val SCHEMA_VERSION = 1

    /** 备份解析结果：骑行记录 + 常用地址。 */
    data class Payload(
        val records: List<Record>,
        val frequentAddresses: List<FrequentAddress>
    )

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(
        records: List<Record>,
        device: String,
        frequentAddresses: List<FrequentAddress> = emptyList()
    ): String {
        val file = BackupFile(
            exportedAt = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            device = device,
            records = records.map { it.toBackupRecord() },
            frequentAddresses = frequentAddresses
        )
        return json.encodeToString(BackupFile.serializer(), file)
    }

    /**
     * 解析备份文件；文件损坏或版本不兼容时抛出 IllegalArgumentException。
     */
    fun decodePayload(text: String): Payload {
        val file = runCatching { json.decodeFromString(BackupFile.serializer(), text) }
            .getOrElse { throw IllegalArgumentException("文件损坏或版本不兼容") }
        require(file.app == APP_ID) { "文件损坏或版本不兼容" }
        require(file.schemaVersion <= SCHEMA_VERSION) { "文件损坏或版本不兼容" }
        return Payload(
            records = file.records.map { it.toRecord() },
            frequentAddresses = file.frequentAddresses
                .filter { it.name.isNotBlank() }
                .map { it.copy(name = it.name.take(16)) }
        )
    }

    fun decode(text: String): List<Record> = decodePayload(text).records

    fun decodeCount(text: String): Int? = runCatching { decode(text).size }.getOrNull()

    private fun Record.toBackupRecord(): BackupRecord = BackupRecord(
        id = id,
        date = TimeUtils.formatDate(date),
        time = TimeUtils.formatTime(time),
        distance = distance,
        note = note,
        routeStart = routeStart,
        routeEnd = routeEnd,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun BackupRecord.toRecord(): Record {
        val dateValue = requireNotNull(TimeUtils.parseDate(date)) { "日期格式错误" }
        val timeValue = requireNotNull(TimeUtils.parseTime(time)) { "时间格式错误" }
        require(distance >= 0.0) { "距离数据无效" }
        val now = System.currentTimeMillis()
        return Record(
            id = id,
            date = dateValue,
            time = timeValue,
            distance = distance,
            note = note.take(200),
            routeStart = routeStart.take(30),
            routeEnd = routeEnd.take(30),
            createdAt = if (createdAt > 0) createdAt else now,
            updatedAt = if (updatedAt > 0) updatedAt else now,
            dirty = false
        )
    }

    @Suppress("unused")
    private fun safeDate(value: String): LocalDate? = TimeUtils.parseDate(value)

    @Suppress("unused")
    private fun safeTime(value: String): LocalTime? = TimeUtils.parseTime(value)
}
