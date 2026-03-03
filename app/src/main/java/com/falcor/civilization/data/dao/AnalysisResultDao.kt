package com.falcor.civilization.data.dao

import androidx.room.*
import com.falcor.civilization.data.entities.AnalysisResultEntity

@Dao
interface AnalysisResultDao {
    @Query("SELECT * FROM analysis_results WHERE runId = :runId")
    suspend fun getByRun(runId: String): AnalysisResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: AnalysisResultEntity)
}
