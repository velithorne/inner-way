package com.velithorne.vessel.model

/**
 * Stylized 2.5D lighting intensities (0..1).
 */
data class SeedPodLightingState(
    val rimLight: Float,
    val sideFalloff: Float,
    val coreBloom: Float,
    val shellCatchlight: Float,
    val thermalHotspot: Float,
    val lateralSheen: Float,
    val chamberSpotlight: Float,
)
