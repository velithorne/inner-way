package com.falcor.civilization.data.dao

import androidx.room.*
import com.falcor.civilization.data.entities.ObservationEntity

@Dao
interface ObservationDao {
    @Query("SELECT * FROM observations WHERE runId = :runId ORDER BY t ASC")
    suspend fun getByRun(runId: String): List<ObservationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(observations: List<ObservationEntity>)

    @Query("DELETE FROM observations WHERE runId = :runId")
    suspend fun deleteByRun(runId: String)
}
