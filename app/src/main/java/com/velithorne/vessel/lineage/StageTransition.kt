package com.velithorne.vessel.lineage

import com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage

data class StageTransition(
    val id: Long,
    val timestampMillis: Long,
    val fromStage: SeedPodGrowthStage,
    val toStage: SeedPodGrowthStage,
    val triggeringChannel: String,
    val explanation: String,
)
