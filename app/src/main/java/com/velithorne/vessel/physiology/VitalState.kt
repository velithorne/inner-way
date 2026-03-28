package com.velithorne.vessel.physiology

/**
 * Core homeostatic trio (subset of [SpeciesState]) for modular pipelines.
 * Renderer / evolution phases may sample this slice before full species projection.
 */
data class VitalState(
    val vitality: Float,
    val stress: Float,
    val recovery: Float,
)
