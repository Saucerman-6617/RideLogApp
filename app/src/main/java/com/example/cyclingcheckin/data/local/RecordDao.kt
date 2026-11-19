package com.example.cyclingcheckin.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {

    @Query("SELECT * FROM record WHERE date = :date ORDER BY time DESC, createdAt DESC")
    fun observeByDate(date: String): Flow<List<RecordEntity>>

    @Query(
        "SELECT date, COUNT(*) AS count, SUM(distance) AS distance FROM record " +
            "WHERE date BETWEEN :start AND :end GROUP BY date ORDER BY date"
    )
    fun observeRange(start: String, end: String): Flow<List<DayStatRow>>

    @Query("SELECT * FROM record ORDER BY date DESC, time DESC, createdAt DESC")
    fun observeAll(): Flow<List<RecordEntity>>

    @Query("SELECT * FROM record ORDER BY date ASC, time ASC, createdAt ASC")
    suspend fun getAll(): List<RecordEntity>

    @Query("SELECT COUNT(*) FROM record")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: RecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<RecordEntity>)

    @Update
    suspend fun update(record: RecordEntity)

    @Delete
    suspend fun delete(record: RecordEntity)

    @Query("DELETE FROM record WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM record")
    suspend fun clearAll()

    @Query("UPDATE record SET dirty = 0")
    suspend fun markAllClean()
}
