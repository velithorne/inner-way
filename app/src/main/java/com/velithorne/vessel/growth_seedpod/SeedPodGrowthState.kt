package com.velithorne.vessel.growth_seedpod

import com.velithorne.vessel.progression.StructuralGrowthState

/**
 * Full seed-pod growth snapshot for engine + UI.
 */
data class SeedPodGrowthState(
    val display: SeedPodDisplayState,
    val budget: SeedPodGrowthBudget,
    /** Irreversible progression — persisted; [display.stage] is kept in sync for Room/lineage. */
    val structural: StructuralGrowthState,
)
