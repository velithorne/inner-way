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

    // Phase 4 — camera & inspection
    val defaultZoom: Float = 1f,
    /** Pinch can zoom out this far (total scale = fitZoom × userZoom). */
    val minZoom: Float = 0.38f,
    /** Pinch can zoom in this far. */
    val maxZoom: Float = 5.75f,
    /** Double-tap focus total zoom (clamped to [minZoom, maxZoom]). */
    val focusZoom: Float = 2.35f,
    val maxPanFraction: Float = 0.52f,
    val maxRotationDeg: Float = 12f,
    val maxTiltDeg: Float = 8f,
    val cameraSmoothing: Float = 0.16f,
    val selectionDimAlpha: Float = 0.38f,
    val selectionGlowStrength: Float = 1.35f,
    val organHitPadMultiplier: Float = 1.55f,
    val doubleTapAnimationCoarse: Float = 0.22f,
    val focusTransitionSeconds: Float = 0.38f,
    val thermalHitFullBodyMultiplier: Float = 1f,

    /** @deprecated use [defaultVisualCentroidTargetY] */
    val defaultCompositionY: Float = 0.44f,
    /**
     * Normalized viewport Y (0 = top) where **visual mass** centroid is placed — ~0.43–0.47 = suspended upper-mid.
     */
    val defaultVisualCentroidTargetY: Float = 0.44f,
    /** Fractional inset from chamber edges when fitting core bounds (0..0.5). */
    val defaultFramingMargin: Float = 0.13f,
    /** Upper cap on auto-fit zoom so default view is not overly zoomed-in on tall phones. */
    val defaultFitZoomCap: Float = 1.85f,
    /**
     * Applied to auto-fit zoom for the default whole-specimen view (lower = more zoomed out).
     * User pinch uses [fitZoom] × [userZoom] with [minZoom]/[maxZoom] on the product.
     */
    val defaultFramingFitMultiplier: Float = 0.71f,

    /** Draw core centroid, fit rect, chamber center (development). */
    val showFramingDebug: Boolean = false,
    /** Seed-first validation: chamber bounds, fixed seed anchor, framing centroid (toggle off for release). */
    val showSeedFirstDebug: Boolean = false,

    // Phase 5 — shell / membrane / pathways / atmosphere
    val shellFillOpacityBase: Float = 0.1f,
    val shellFillOpacityVitalityScale: Float = 0.22f,
    val shellFillOpacityMin: Float = 0.06f,
    val shellFillOpacityMax: Float = 0.42f,

    val shellEdgeAlphaBase: Float = 0.16f,
    val shellEdgeAlphaVitalityScale: Float = 0.28f,
    val shellEdgeThicknessMin: Float = 2.4f,
    val shellEdgeThicknessStressScale: Float = 1.6f,

    val innerHazeAlphaBase: Float = 0.04f,
    val thermalTintDisplayScale: Float = 0.95f,
    val thermalEdgeBleedScale: Float = 0.55f,
    val thermalShimmerDisplayScale: Float = 0.85f,
    val recoverySheenScale: Float = 0.65f,

    val pathwayBaseAlpha: Float = 0.14f,
    val pathwayPulseSpeed: Float = 1.15f,
    val pathwayWidthMetabolic: Float = 1.6f,
    val pathwayWidthNeural: Float = 1.35f,

    val organHaloBase: Float = 0.92f,
    val chamberFogDepthNearMul: Float = 0.65f,
    val chamberFogDepthFarMul: Float = 1.15f,
    val reflectionSweepAlpha: Float = 0.055f,
    val scanSheenAlpha: Float = 0.035f,

    val selectionNonSelectedDim: Float = 0.32f,
    val selectionFocusIntensity: Float = 1.15f,

    val rearParallaxMul: Float = 0.06f,
    val shellParallaxMul: Float = 0.11f,
    val organParallaxMul: Float = 0.14f,
)
