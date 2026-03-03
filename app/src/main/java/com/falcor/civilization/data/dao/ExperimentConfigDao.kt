package com.falcor.civilization.data.dao

import androidx.room.*
import com.falcor.civilization.data.entities.ExperimentConfigEntity

@Dao
interface ExperimentConfigDao {
    @Query("SELECT * FROM experiment_configs WHERE preregPlanId = :preregPlanId")
    suspend fun getByPreregPlan(preregPlanId: String): List<ExperimentConfigEntity>

    @Query("SELECT * FROM experiment_configs WHERE id = :id")
    suspend fun getById(id: String): ExperimentConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: ExperimentConfigEntity)
}
