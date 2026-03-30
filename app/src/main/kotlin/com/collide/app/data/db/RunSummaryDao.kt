package com.collide.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RunSummaryDao {
    @Query("SELECT * FROM run_summaries ORDER BY timestamp DESC")
    fun getAllSummaries(): Flow<List<RunSummaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: RunSummaryEntity): Long

    @Query("SELECT COUNT(*) FROM run_summaries")
    fun getSummaryCount(): Flow<Int>
}
