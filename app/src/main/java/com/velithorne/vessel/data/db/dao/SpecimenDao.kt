package com.velithorne.vessel.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.velithorne.vessel.data.db.entity.SpecimenEntity

@Dao
interface SpecimenDao {
    @Query("SELECT * FROM specimens WHERE active = 1 LIMIT 1")
    suspend fun getActiveSpecimen(): SpecimenEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SpecimenEntity)

    @Query("DELETE FROM specimens")
    suspend fun deleteAll()
}
