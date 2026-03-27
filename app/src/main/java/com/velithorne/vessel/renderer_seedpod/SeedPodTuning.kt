package com.velithorne.vessel.renderer_seedpod

/**
 * Seed pod chamber — **no** legacy vessel [com.velithorne.vessel.renderer.RenderTuning].
 */
data class SeedPodTuning(
    val minZoom: Float = 0.42f,
    val maxZoom: Float = 5.5f,
    val defaultZoom: Float = 1f,
    val defaultFitZoom: Float = 1f,
    val maxPanFraction: Float = 0.52f,
    val maxRotationDeg: Float = 12f,
    val maxTiltDeg: Float = 8f,
    val cameraSmoothing: Float = 0.16f,
    val stressShiverDegrees: Float = 1.2f,
    /** Pod nucleus radius as fraction of min(w,h). */
    val podCoreRadiusMul: Float = 0.065f,
    val podShellRadiusMul: Float = 0.11f,
    /** Debug: chamber rect, anchor crosshair, stage label position — default OFF. */
    val showSeedPodDebug: Boolean = false,
)
