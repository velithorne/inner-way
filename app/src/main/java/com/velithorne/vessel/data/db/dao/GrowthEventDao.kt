package com.velithorne.vessel.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.velithorne.vessel.data.db.entity.GrowthEventEntity
import com.velithorne.vessel.data.db.entity.GrowthStageEventEntity

@Dao
interface GrowthEventDao {
    @Insert
    suspend fun insertGrowthEvent(e: GrowthEventEntity): Long

    @Insert
    suspend fun insertStageEvent(e: GrowthStageEventEntity): Long

    @Query(
        "SELECT * FROM growth_stage_events WHERE specimenId = :id ORDER BY timestampMillis DESC LIMIT :limit",
    )
    suspend fun recentStageEvents(id: String, limit: Int): List<GrowthStageEventEntity>

    @Query(
        "SELECT * FROM growth_events WHERE specimenId = :id ORDER BY timestampMillis DESC LIMIT :limit",
    )
    suspend fun recentGrowthEvents(id: String, limit: Int): List<GrowthEventEntity>

    @Query("DELETE FROM growth_events")
    suspend fun deleteAllGrowth()

    @Query("DELETE FROM growth_stage_events")
    suspend fun deleteAllStage()

}
