package com.velithorne.vessel.model

/**
 * Safe subset of birth traits for release UI (no full hidden vector).
 */
data class BirthTraitState(
    val symmetryHint: Float,
    val coherenceHint: Float,
    val asymmetryHint: Float,
)
