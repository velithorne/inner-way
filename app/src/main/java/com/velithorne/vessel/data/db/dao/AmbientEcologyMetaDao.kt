package com.velithorne.vessel.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.velithorne.vessel.data.db.entity.AmbientEcologyMetaEntity

@Dao
interface AmbientEcologyMetaDao {
    @Query("SELECT * FROM ambient_ecology_meta WHERE specimenId = :id")
    suspend fun get(id: String): AmbientEcologyMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: AmbientEcologyMetaEntity)

    @Query("DELETE FROM ambient_ecology_meta")
    suspend fun deleteAll()
}
