package com.velithorne.vessel.growth_seedpod

/**
 * Local developmental budgets (0..1) accumulated from physiology pressures.
 */
data class SeedPodGrowthBudget(
    val crown: Float = 0f,
    val lateral: Float = 0f,
    val reserve: Float = 0f,
    val shell: Float = 0f,
    val thermal: Float = 0f,
    val coherence: Float = 0f,
)
