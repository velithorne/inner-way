package com.collide.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)

    @Query("SELECT * FROM events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEventById(id: String): EventEntity?

    @Query("SELECT COUNT(*) FROM events")
    fun getEventCount(): Flow<Int>

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteEvent(id: String)

    @Update
    suspend fun updateEvent(event: EventEntity)
}

@Dao
interface RunSummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRunSummary(summary: RunSummaryEntity)

    @Query("SELECT * FROM run_summaries ORDER BY timestamp DESC")
    fun getAllRunSummaries(): Flow<List<RunSummaryEntity>>

    @Query("SELECT * FROM run_summaries ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestRunSummary(): RunSummaryEntity?
}

@Dao
interface NearMissDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNearMiss(nearMiss: NearMissEntity)

    @Query("SELECT * FROM near_misses WHERE runId = :runId")
    suspend fun getNearMissesForRun(runId: String): List<NearMissEntity>

    @Query("DELETE FROM near_misses WHERE runId = :runId")
    suspend fun deleteNearMissesForRun(runId: String)
}
