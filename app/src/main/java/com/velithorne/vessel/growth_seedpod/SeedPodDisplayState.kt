package com.velithorne.vessel.growth_seedpod

/**
 * Lagging display state for the seed pod (what we draw).
 */
data class SeedPodDisplayState(
    val stage: SeedPodGrowthStage,
    /** 0..1 — crown bud above pod */
    val crownNub: Float,
    /** 0..1 — left / right signal buds */
    val lateralBudLeft: Float,
    val lateralBudRight: Float,
    /** 0..1 — lower reserve droplet */
    val reserveBulb: Float,
    /** 0..1 — shell edge thickness / opacity */
    val shellThickening: Float,
    /** 0..1 — thermal veil around perimeter */
    val thermalVeil: Float,
    /** 0..1 — tissue haze envelope */
    val tissueHaze: Float,
    /** 0..1 — shell smoothness / coherence */
    val podCoherence: Float,
    val lastWallClockMs: Long,
)
