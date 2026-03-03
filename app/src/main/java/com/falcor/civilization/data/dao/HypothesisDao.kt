package com.falcor.civilization.data.dao

import androidx.room.*
import com.falcor.civilization.data.entities.HypothesisEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HypothesisDao {
    @Query("SELECT * FROM hypotheses WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getByProject(projectId: String): Flow<List<HypothesisEntity>>

    @Query("SELECT * FROM hypotheses WHERE id = :id")
    suspend fun getById(id: String): HypothesisEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hypothesis: HypothesisEntity)

    @Update
    suspend fun update(hypothesis: HypothesisEntity)
}
