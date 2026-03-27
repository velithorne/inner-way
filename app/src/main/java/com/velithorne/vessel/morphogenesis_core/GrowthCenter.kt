package com.velithorne.vessel.morphogenesis_core

/**
 * Active locus of self-assembly — feeds local field and renders as bright micro-locus.
 */
data class GrowthCenter(
    val id: String,
    val nx: Float,
    val ny: Float,
    val intensity: Float,
    val phase: Float,
    val channel: PressureChannel,
)
