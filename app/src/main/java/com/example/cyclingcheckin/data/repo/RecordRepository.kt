package com.example.cyclingcheckin.data.repo

import com.example.cyclingcheckin.data.local.RecordDao
import com.example.cyclingcheckin.data.local.RecordEntity
import com.example.cyclingcheckin.domain.model.DayStat
import com.example.cyclingcheckin.domain.model.ImportResult
import com.example.cyclingcheckin.domain.model.Record
import com.example.cyclingcheckin.domain.model.Validators
import com.example.cyclingcheckin.util.TimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime

interface RecordRepository {
    fun observeByDate(date: LocalDate): Flow<List<Record>>
    fun observeRange(start: LocalDate, end: LocalDate): Flow<List<DayStat>>
    fun observeAll(): Flow<List<Record>>
    suspend fun getAll(): List<Record>
    suspend fun create(date: LocalDate, time: LocalTime, distance: Double, note: String): Result<Record>
    suspend fun update(record: Record): Result<Unit>
    suspend fun delete(record: Record): Result<Unit>
    suspend fun restore(record: Record): Result<Unit>
    suspend fun deleteById(id: Long)
    suspend fun markAllClean()
    suspend fun clearAll()
    suspend fun importRecords(records: List<Record>, overwrite: Boolean): ImportResult
}

class RecordRepositoryImpl(
    private val dao: RecordDao
) : RecordRepository {

    override fun observeByDate(date: LocalDate): Flow<List<Record>> =
        dao.observeByDate(TimeUtils.formatDate(date)).map { list -> list.map { it.toDomain() } }

    override fun observeRange(start: LocalDate, end: LocalDate): Flow<List<DayStat>> =
        dao.observeRange(TimeUtils.formatDate(start), TimeUtils.formatDate(end)).map { rows ->
            rows.mapNotNull { row ->
                val date = TimeUtils.parseDate(row.date) ?: return@mapNotNull null
                DayStat(
                    date = date,
                    count = row.count,
                    distance = Validators.roundDistance(row.distance)
                )
            }
        }

    override fun observeAll(): Flow<List<Record>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAll(): List<Record> = dao.getAll().map { it.toDomain() }

    override suspend fun create(
        date: LocalDate,
        time: LocalTime,
        distance: Double,
        note: String
    ): Result<Record> = runCatching {
        val now = System.currentTimeMillis()
        val entity = RecordEntity(
            date = TimeUtils.formatDate(date),
            time = TimeUtils.formatTime(time),
            distance = Validators.roundDistance(distance),
            note = note.trim().take(Validators.MAX_NOTE_LENGTH),
            createdAt = now,
            updatedAt = now,
            dirty = true
        )
        val id = dao.insert(entity)
        entity.copy(id = id).toDomain()
    }

    override suspend fun update(record: Record): Result<Unit> = runCatching {
        dao.update(
            RecordEntity(
                id = record.id,
                date = TimeUtils.formatDate(record.date),
                time = TimeUtils.formatTime(record.time),
                distance = Validators.roundDistance(record.distance),
                note = record.note.trim().take(Validators.MAX_NOTE_LENGTH),
                createdAt = record.createdAt,
                updatedAt = System.currentTimeMillis(),
                dirty = true
            )
        )
    }

    override suspend fun delete(record: Record): Result<Unit> = runCatching {
        dao.delete(record.toEntity())
    }

    override suspend fun restore(record: Record): Result<Unit> = runCatching {
        dao.insert(record.toEntity())
        Unit
    }

    override suspend fun deleteById(id: Long) = dao.deleteById(id)

    override suspend fun markAllClean() = dao.markAllClean()

    override suspend fun clearAll() = dao.clearAll()

    override suspend fun importRecords(
        records: List<Record>,
        overwrite: Boolean
    ): ImportResult {
        if (overwrite) {
            dao.clearAll()
            val entities = records.map { it.copy(id = 0).toEntity() }
            dao.insertAll(entities)
            return ImportResult(imported = records.size, skipped = 0)
        }
        val existing = dao.getAll().map { it.dedupeKey() }.toMutableSet()
        var imported = 0
        var skipped = 0
        records.forEach { record ->
            val key = record.dedupeKey()
            if (existing.contains(key)) {
                skipped++
            } else {
                existing.add(key)
                dao.insert(record.copy(id = 0).toEntity())
                imported++
            }
        }
        return ImportResult(imported = imported, skipped = skipped)
    }
}

private fun String.dedupeKey(): String = this

private fun Record.dedupeKey(): String =
    "${TimeUtils.formatDate(date)}|${TimeUtils.formatTime(time)}|${Validators.roundDistance(distance)}"

private fun RecordEntity.dedupeKey(): String =
    "$date|$time|${Validators.roundDistance(distance)}"

private fun RecordEntity.toDomain(): Record = Record(
    id = id,
    date = TimeUtils.parseDate(date) ?: LocalDate.now(),
    time = TimeUtils.parseTime(time) ?: LocalTime.MIDNIGHT,
    distance = distance,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
    dirty = dirty
)

private fun Record.toEntity(): RecordEntity = RecordEntity(
    id = id,
    date = TimeUtils.formatDate(date),
    time = TimeUtils.formatTime(time),
    distance = distance,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
    dirty = dirty
)
