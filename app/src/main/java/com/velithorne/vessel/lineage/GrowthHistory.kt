package com.velithorne.vessel.lineage

/** Recent window of history for UI lists. */
data class GrowthHistory(
    val stageTransitions: List<StageTransition>,
    val growthEvents: List<GrowthEvent>,
)
