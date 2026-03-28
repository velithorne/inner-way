package com.velithorne.vessel.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.velithorne.vessel.data.db.entity.SeedPodStateEntity

@Dao
interface SeedPodStateDao {
    @Query("SELECT * FROM seed_pod_state WHERE specimenId = :id LIMIT 1")
    suspend fun getForSpecimen(id: String): SeedPodStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SeedPodStateEntity)

    @Query("DELETE FROM seed_pod_state")
    suspend fun deleteAll()
}
