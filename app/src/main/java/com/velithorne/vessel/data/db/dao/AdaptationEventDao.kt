package com.velithorne.vessel.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.velithorne.vessel.data.db.entity.AdaptationEventEntity

@Dao
interface AdaptationEventDao {
    @Query("SELECT * FROM adaptation_markers WHERE specimenId = :id")
    suspend fun getAllForSpecimen(id: String): List<AdaptationEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AdaptationEventEntity)

    @Query("DELETE FROM adaptation_markers")
    suspend fun deleteAll()
}
