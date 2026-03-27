package com.velithorne.vessel.morphogenesis_core

/**
 * Scalar tissue channels sampled in normalized body space.
 */
data class TissueField(
    val density: Float,
    val reserve: Float,
    val signal: Float,
    val thermalTension: Float,
    val supportTension: Float,
    val archiveBurden: Float,
    val repairDebt: Float,
    val shellPressure: Float,
    val mutationPressure: Float,
)
