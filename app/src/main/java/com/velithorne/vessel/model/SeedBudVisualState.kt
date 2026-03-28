package com.velithorne.vessel.model

/**
 * Effective bud strengths after stage gates and visibility thresholds (what we actually draw).
 */
data class SeedBudVisualState(
    val crown: Float,
    val lateralLeft: Float,
    val lateralRight: Float,
    val reserve: Float,
)
