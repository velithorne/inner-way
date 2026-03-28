package com.velithorne.vessel.morphogenesis

/**
 * Instant per-tick pressures derived from telemetry + physiology (0..1).
 */
data class GrowthPressure(
    val thermalPressure: Float,
    val reservePressure: Float,
    val hungerPressure: Float,
    val signalPressure: Float,
    val archivePressure: Float,
    val motionPressure: Float,
    val neuralPressure: Float,
    val recoveryPressure: Float,
    val sleepPressure: Float,
    val environmentalAttachmentPressure: Float,
)
