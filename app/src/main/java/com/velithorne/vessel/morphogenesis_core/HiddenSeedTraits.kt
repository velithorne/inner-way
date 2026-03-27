package com.velithorne.vessel.morphogenesis_core

/**
 * Deterministic hidden traits from specimen id + build — stable per specimen.
 */
data class HiddenSeedTraits(
    val symmetryJitter: Float,
    val densityNoise: Float,
    val frondChaos: Float,
    val shellGrain: Float,
    val coherenceDrift: Float,
)
