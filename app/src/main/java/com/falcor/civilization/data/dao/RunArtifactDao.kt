package com.falcor.civilization.data.dao

import androidx.room.*
import com.falcor.civilization.data.entities.RunArtifactEntity

@Dao
interface RunArtifactDao {
    @Query("SELECT * FROM run_artifacts WHERE runId = :runId")
    suspend fun getByRun(runId: String): List<RunArtifactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(artifact: RunArtifactEntity)
}
