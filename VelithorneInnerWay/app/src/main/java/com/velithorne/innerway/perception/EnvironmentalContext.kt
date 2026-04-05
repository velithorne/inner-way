package com.velithorne.innerway.perception

/**
 * Fused snapshot of body-reported conditions (truth layer before interpretation).
 */
data class EnvironmentalContext(
    val energyRatio: Float = 1f,
    val charging: Boolean = false,
    val thermalRatio: Float = 0.35f,
    val nervousLoad: Float = 0.2f,
    val storageFreeRatio: Float = 0.5f,
    val motionEnergy: Float = 0f,
    val networkOpenness: Float = 0.5f,
    val circadianPhase: Float = 0.5f,
    val screenAwake: Boolean = true,
    val timestampMillis: Long = System.currentTimeMillis(),
)
