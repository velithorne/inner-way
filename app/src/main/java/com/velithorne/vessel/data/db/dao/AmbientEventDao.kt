package com.velithorne.vessel.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.velithorne.vessel.data.db.entity.AmbientEventEntity

@Dao
interface AmbientEventDao {
    @Insert
    suspend fun insert(e: AmbientEventEntity): Long

    @Query("SELECT COUNT(*) FROM ambient_events WHERE specimenId = :id")
    suspend fun countForSpecimen(id: String): Int

    @Query(
        """
        DELETE FROM ambient_events WHERE specimenId = :id AND id IN (
            SELECT id FROM ambient_events WHERE specimenId = :id ORDER BY timestampMillis ASC LIMIT :excess
        )
        """,
    )
    suspend fun deleteOldestExcess(id: String, excess: Int)

    @Query("SELECT * FROM ambient_events WHERE specimenId = :id ORDER BY timestampMillis DESC LIMIT :limit")
    suspend fun recent(id: String, limit: Int): List<AmbientEventEntity>

    @Query("DELETE FROM ambient_events")
    suspend fun deleteAll()
}
