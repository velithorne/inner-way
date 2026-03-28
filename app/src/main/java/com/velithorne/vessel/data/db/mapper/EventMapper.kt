package com.velithorne.vessel.data.db.mapper

import com.velithorne.vessel.data.db.entity.GrowthEventEntity
import com.velithorne.vessel.data.db.entity.GrowthStageEventEntity
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage
import com.velithorne.vessel.lineage.GrowthEvent
import com.velithorne.vessel.lineage.GrowthEventType
import com.velithorne.vessel.lineage.StageTransition

object EventMapper {

    fun stageToDomain(e: GrowthStageEventEntity): StageTransition =
        StageTransition(
            id = e.id,
            timestampMillis = e.timestampMillis,
            fromStage = SeedPodGrowthStage.entries.getOrNull(e.fromStageOrdinal) ?: SeedPodGrowthStage.DORMANT_POD,
            toStage = SeedPodGrowthStage.entries.getOrNull(e.toStageOrdinal) ?: SeedPodGrowthStage.DORMANT_POD,
            triggeringChannel = e.triggeringChannel,
            explanation = e.explanation,
        )

    fun growthToDomain(e: GrowthEventEntity): GrowthEvent =
        GrowthEvent(
            id = e.id,
            timestampMillis = e.timestampMillis,
            eventType = GrowthEventType.entries.getOrNull(e.eventTypeOrdinal) ?: GrowthEventType.CUSTOM,
            affectedRegion = e.affectedRegion,
            magnitude = e.magnitude,
            primaryDriver = e.primaryDriver,
            explanation = e.explanation,
            occurredDuringOfflineCatchUp = e.offlineCatchUp,
        )
}
