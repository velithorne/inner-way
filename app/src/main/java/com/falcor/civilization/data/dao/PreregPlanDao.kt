package com.falcor.civilization.data.dao

import androidx.room.*
import com.falcor.civilization.data.entities.PreregPlanEntity

@Dao
interface PreregPlanDao {
    @Query("SELECT * FROM prereg_plans WHERE hypothesisId = :hypothesisId ORDER BY frozenAt DESC LIMIT 1")
    suspend fun getLatestForHypothesis(hypothesisId: String): PreregPlanEntity?

    @Query("SELECT * FROM prereg_plans WHERE id = :id")
    suspend fun getById(id: String): PreregPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: PreregPlanEntity)
}
