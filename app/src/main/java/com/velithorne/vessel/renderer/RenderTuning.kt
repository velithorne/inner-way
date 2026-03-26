package com.velithorne.vessel.renderer

/**
 * Tuning for 2.5D vessel presentation. Adjust for device performance or art direction.
 *
 * Future: load from DataStore; evolution may unlock alternate palettes (not in Phase 3).
 */
data class RenderTuning(
    val pulseFrequencyHz: Float = 1.05f,
    val breathFrequencyHz: Float = 0.18f,
    val pulseAmplitudeVitalityScale: Float = 0.085f,
    val pulseAmplitudeStressBoost: Float = 0.04f,
    val breathAmplitudeRespirationScale: Float = 0.06f,
    val feverThermalTint: Float = 0.85f,
    val feverShimmerScale: Float = 0.7f,
    val vitalityGlowScale: Float = 1.2f,
    val stressShiverDegrees: Float = 1.8f,
    val neuralFlickerHz: Float = 3.2f,
    val neuralArcIntensity: Float = 0.55f,
    val particleCount: Int = 56,
    val particleBaseSpeed: Float = 22f,
    val fogDensityScale: Float = 1f,
    val parallaxMaxPx: Float = 28f,
    val parallaxSmoothing: Float = 0.14f,
    val organScaleGlobal: Float = 1f,
    val sleepDimMax: Float = 0.42f,
    val recoveryShimmerScale: Float = 0.65f,
    val signalCyanBoost: Float = 0.9f,
    val bodyProfileScale: Float = 1f,
)
