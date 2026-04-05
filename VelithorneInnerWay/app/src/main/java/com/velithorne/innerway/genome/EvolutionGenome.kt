package com.velithorne.innerway.genome

/**
 * Growth thresholds and resilience; co-evolves with [com.velithorne.innerway.mind.EvolutionEngine].
 */
data class EvolutionGenome(
    val unlockMemoryFloor: Int = 8,
    val stableCyclesRequired: Int = 3,
    val lowStressWindowsRequired: Int = 2,
    val carePatternFloor: Int = 1,
    val survivalHistoryFloor: Int = 1,
    val mutationTendency: Float = 0.08f,
    val resilience: Float = 0.55f,
    val traumaAccumulation: Float = 0f,
    val recoveryRate: Float = 0.12f,
)
