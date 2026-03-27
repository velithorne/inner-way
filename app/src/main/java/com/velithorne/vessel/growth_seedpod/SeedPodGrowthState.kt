package com.velithorne.vessel.growth_seedpod

/**
 * Full seed-pod growth snapshot for engine + UI.
 */
data class SeedPodGrowthState(
    val display: SeedPodDisplayState,
    val budget: SeedPodGrowthBudget,
)
