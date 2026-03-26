package com.velithorne.vessel.model

import com.velithorne.vessel.morphogenesis.GerminationStage
import com.velithorne.vessel.morphogenesis.GrowthFrontType

/**
 * UI + renderer growth stage and growth-front hints.
 */
data class GrowthStageVisualState(
    val stage: GerminationStage,
    val stageLabel: String,
    val growthFrontIntensity: Float,
    val growthFrontDirX: Float,
    val growthFrontDirY: Float,
    val primaryFront: GrowthFrontType,
    val secondaryFront: GrowthFrontType,
)
