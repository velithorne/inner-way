package com.velithorne.vessel.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.velithorne.vessel.data.db.entity.EcologySnapshotEntity

@Dao
interface EcologySnapshotDao {
    @Insert
    suspend fun insert(e: EcologySnapshotEntity): Long

    @Query("SELECT COUNT(*) FROM ecology_snapshots WHERE specimenId = :id")
    suspend fun countForSpecimen(id: String): Int

    @Query(
        """
        DELETE FROM ecology_snapshots WHERE specimenId = :id AND id IN (
            SELECT id FROM ecology_snapshots WHERE specimenId = :id ORDER BY timestampMillis ASC LIMIT :excess
        )
        """,
    )
    suspend fun deleteOldestExcess(id: String, excess: Int)

    @Query("SELECT * FROM ecology_snapshots WHERE specimenId = :id AND timestampMillis > :sinceMs ORDER BY timestampMillis ASC")
    suspend fun since(id: String, sinceMs: Long): List<EcologySnapshotEntity>

    @Query("SELECT * FROM ecology_snapshots WHERE specimenId = :id ORDER BY timestampMillis DESC LIMIT :limit")
    suspend fun recent(id: String, limit: Int): List<EcologySnapshotEntity>

    @Query("DELETE FROM ecology_snapshots")
    suspend fun deleteAll()
}
