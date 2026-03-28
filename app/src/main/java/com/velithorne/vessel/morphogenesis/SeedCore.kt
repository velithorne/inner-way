package com.velithorne.vessel.morphogenesis

/**
 * Crystalline-biological seed nucleus — latent structure at the center of the species.
 */
data class SeedCore(
    /** Normalized core radius in body space (0..1 scale vs specimen). */
    val coreRadius: Float,
    val seedDensity: Float,
    val reserveLuminance: Float,
    val shellCoherence: Float,
    /** 0 = latent, 1 = fully unfolded germination. */
    val germinationProgress: Float,
    val branchLatentEnergy: Float,
    val archiveLatentMass: Float,
    val signalLatentBias: Float,
    val thermalAdaptationBias: Float,
    /** Subtle instability under stress (0..1). */
    val latticeStress: Float,
)
