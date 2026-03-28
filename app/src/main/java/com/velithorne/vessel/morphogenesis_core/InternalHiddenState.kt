package com.velithorne.vessel.morphogenesis_core

/**
 * Hidden topology drivers — updated from pressure + biography, not live UI alone.
 */
data class InternalHiddenState(
    val reserveLevel: Float = 0.5f,
    val thermalTension: Float = 0f,
    val signalCoherence: Float = 0.5f,
    val repairDebt: Float = 0f,
    val growthPressure: Float = 0f,
    val lineageConfidence: Float = 0.35f,
)
