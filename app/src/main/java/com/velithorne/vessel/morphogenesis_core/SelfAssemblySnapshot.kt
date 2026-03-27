package com.velithorne.vessel.morphogenesis_core

import com.velithorne.vessel.model.GeneratedAnatomyState

data class SelfAssemblySnapshot(
    val archetype: SeedArchetype,
    val pressure: GrowthPressureState,
    val hidden: InternalHiddenState,
    val biography: BiographyState,
    val anatomy: GeneratedAnatomyState,
)
