package com.collide.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CorpusRunSummaryDao {
    @Query("SELECT * FROM corpus_run_summaries ORDER BY timestamp DESC")
    fun getAllSummaries(): Flow<List<CorpusRunSummaryEntity>>

    @Query("SELECT * FROM corpus_run_summaries ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): CorpusRunSummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CorpusRunSummaryEntity): Long

    @Query("SELECT COUNT(*) FROM corpus_run_summaries")
    fun getCount(): Flow<Int>
}
