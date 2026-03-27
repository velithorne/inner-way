package com.velithorne.vessel.model

/**
 * Derived appearance scalars for the seed pod canvas (material + stage + physiology).
 */
data class SeedPodVisualState(
    /** Shell membrane alpha multiplier (translucency inverse). */
    val shellOpacityMul: Float,
    /** Rim / conductive seam brightness. */
    val shellEdgeBright: Float,
    /** Inner chamber haze density 0..1. */
    val innerHazeDensity: Float,
    /** Nucleus core brightness multiplier. */
    val nucleusBrightnessMul: Float,
    /** Nucleus bloom / secondary glow strength. */
    val nucleusBloomMul: Float,
    /** Facet / crystalline line visibility. */
    val facetLineAlpha: Float,
    /** Growth-front shimmer strength. */
    val growthFrontAlpha: Float,
    /** Chamber spotlight strength on specimen. */
    val spotlightStrength: Float,
    /** Glass reflection alpha for chamber frame. */
    val glassReflectionAlpha: Float,
    /** Particle density scale. */
    val particleScale: Float,
    /** Stage-based global scale for bud visibility (0..1+). */
    val stageBudScale: Float,
    /** Tight “closed” look for dormant (reduces shell separation visual). */
    val shellClosedness: Float,
)
