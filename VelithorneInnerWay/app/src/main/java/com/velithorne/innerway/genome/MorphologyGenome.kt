package com.velithorne.innerway.genome

/**
 * Visual identity parameters (pulse density, contraction, palette logic).
 * Phase 5 wires visuals; persisted when morphology mutates.
 */
data class MorphologyGenome(
    val pulseDensity: Float = 0.45f,
    val contractionBias: Float = 0.4f,
    val shapeStage: Int = 0,
    val hueShift: Float = 0.12f,
    val textureGrain: Float = 0.2f,
)
