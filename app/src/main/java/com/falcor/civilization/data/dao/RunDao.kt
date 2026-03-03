package com.falcor.civilization.data.dao

import androidx.room.*
import com.falcor.civilization.data.entities.RunEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunDao {
    @Query("SELECT * FROM runs WHERE projectId = :projectId ORDER BY startedAt DESC")
    fun getByProject(projectId: String): Flow<List<RunEntity>>

    @Query("SELECT * FROM runs WHERE id = :id")
    suspend fun getById(id: String): RunEntity?

    @Query("SELECT * FROM runs WHERE status = :status AND projectId = :projectId")
    suspend fun getByStatus(projectId: String, status: String): List<RunEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(run: RunEntity)

    @Update
    suspend fun update(run: RunEntity)
}
