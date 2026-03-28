package com.velithorne.vessel.growth_seedpod

import com.velithorne.vessel.morphogenesis_core.SelfAssemblySnapshot
import com.velithorne.vessel.progression.StructuralGrowthState

/**
 * Full seed-pod growth snapshot for engine + UI.
 */
data class SeedPodGrowthState(
    val display: SeedPodDisplayState,
    val budget: SeedPodGrowthBudget,
    /** Irreversible progression — persisted; [display.stage] is kept in sync for Room/lineage. */
    val structural: StructuralGrowthState,
    /** Encoded [com.velithorne.vessel.morphogenesis_core.MorphogenesisPersistenceCodec] — pressure + hidden + biography flags. */
    val morphogenesisBlob: String? = null,
    /** Latest procedural self-assembly frame — not persisted (rebuilt from engines each tick). */
    val lastSelfAssembly: SelfAssemblySnapshot? = null,
)
