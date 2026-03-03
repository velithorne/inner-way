package com.falcor.civilization.data.dao

import androidx.room.*
import com.falcor.civilization.data.entities.FindingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FindingDao {
    @Query("SELECT * FROM findings WHERE hypothesisId = :hypothesisId AND retiredAt IS NULL")
    fun getActiveByHypothesis(hypothesisId: String): Flow<List<FindingEntity>>

    @Query("SELECT * FROM findings WHERE retiredAt IS NULL ORDER BY promotedAt DESC")
    fun getAllActiveFindings(): Flow<List<FindingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(finding: FindingEntity)

    @Update
    suspend fun update(finding: FindingEntity)
}
