package com.velithorne.vessel.genesis

import com.velithorne.vessel.morphogenesis_core.SeedArchetype
import com.velithorne.vessel.morphogenesis_core.StructuralGraph

/**
 * Full genesis layer output for renderer + UI (birth / early seed era).
 */
data class SeedGenesisState(
    val traits: HiddenSeedTraits,
    val birthPressure: BirthPressureState,
    val minimumViableBody: MinimumViableBody,
    val archetype: SeedArchetype,
    val graph: StructuralGraph,
    val genesisContourDriver: String,
    val birthTendencyLine: String,
)
