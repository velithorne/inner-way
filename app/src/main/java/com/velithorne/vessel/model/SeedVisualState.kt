package com.velithorne.vessel.model

/**
 * Renderer-ready seed nucleus parameters (from morphogenesis [com.velithorne.vessel.morphogenesis.SeedCore]).
 */
data class SeedVisualState(
    val coreRadiusNorm: Float,
    val seedDensity: Float,
    val reserveLuminance: Float,
    val shellCoherence: Float,
    val germinationProgress: Float,
    val branchLatentEnergy: Float,
    val latticeStress: Float,
)
