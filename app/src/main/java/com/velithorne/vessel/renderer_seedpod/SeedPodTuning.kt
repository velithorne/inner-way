package com.velithorne.vessel.renderer_seedpod

/**
 * Seed pod chamber — grouped tuning (no scattered magic numbers in painters).
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
    val podCoreRadiusMul: Float = 0.058f,
    val podShellRadiusMul: Float = 0.118f,
    /** Vesica-like vertical stretch of outer shell (1 = circle). */
    val shellVesicaStretchY: Float = 1.08f,
    val shellVesicaStretchX: Float = 0.96f,
    /** Membrane ring radii as fractions of minDim (outer … inner). */
    val shellBandOuterMul: Float = 1.12f,
    val shellBandMidMul: Float = 1.02f,
    val shellBandInnerMul: Float = 0.88f,
    /** Bud visibility: raw display value must exceed threshold before drawing / explainer. */
    val budVisibilityThresholdCrown: Float = 0.22f,
    val budVisibilityThresholdLateral: Float = 0.2f,
    val budVisibilityThresholdReserve: Float = 0.22f,
    val crownBudSizeCurve: Float = 1.15f,
    val lateralBudSizeCurve: Float = 1.1f,
    val reserveBudSizeCurve: Float = 1.05f,
    val thermalShimmerStrength: Float = 0.85f,
    val growthFrontAlphaMax: Float = 0.55f,
    val glassReflectionBase: Float = 0.1f,
    val chamberSpotlightFalloff: Float = 0.42f,
    val innerHazeMax: Float = 0.38f,
    val facetBaseAlpha: Float = 0.12f,
    val nucleusFacetCount: Int = 6,
    val conductiveSeamCount: Int = 8,
    val stageVisual: StageVisualGroup = StageVisualGroup(),
    val showSeedPodDebug: Boolean = false,
)

/**
 * Per-stage multipliers so DORMANT … EARLY_CHAMBERING read differently on canvas.
 */
data class StageVisualGroup(
    val dormantShellDim: Float = 0.72f,
    val dormantEdge: Float = 0.05f,
    val dormantHaze: Float = 0.35f,
    val dormantNucleus: Float = -0.12f,
    val dormantFacet: Float = -0.04f,
    val dormantGrowthFront: Float = 0.02f,
    val dormantSpotlight: Float = -0.08f,
    val dormantBudScale: Float = 0.35f,
    val dormantClosedness: Float = 0.92f,

    val activatingShellDim: Float = 0.88f,
    val activatingEdge: Float = 0.12f,
    val activatingHaze: Float = 0.55f,
    val activatingNucleus: Float = 0.05f,
    val activatingFacet: Float = 0.02f,
    val activatingGrowthFront: Float = 0.08f,
    val activatingSpotlight: Float = 0f,
    val activatingBudScale: Float = 0.55f,
    val activatingClosedness: Float = 0.78f,

    val germinatingShellDim: Float = 0.98f,
    val germinatingEdge: Float = 0.18f,
    val germinatingHaze: Float = 0.85f,
    val germinatingNucleus: Float = 0.12f,
    val germinatingFacet: Float = 0.08f,
    val germinatingGrowthFront: Float = 0.18f,
    val germinatingSpotlight: Float = 0.08f,
    val germinatingBudScale: Float = 0.82f,
    val germinatingClosedness: Float = 0.55f,

    val buddingShellDim: Float = 1f,
    val buddingEdge: Float = 0.22f,
    val buddingHaze: Float = 1f,
    val buddingNucleus: Float = 0.18f,
    val buddingFacet: Float = 0.12f,
    val buddingGrowthFront: Float = 0.28f,
    val buddingSpotlight: Float = 0.12f,
    val buddingBudScale: Float = 1f,
    val buddingClosedness: Float = 0.35f,

    val chamberingShellDim: Float = 1.02f,
    val chamberingEdge: Float = 0.2f,
    val chamberingHaze: Float = 1.12f,
    val chamberingNucleus: Float = 0.15f,
    val chamberingFacet: Float = 0.1f,
    val chamberingGrowthFront: Float = 0.32f,
    val chamberingSpotlight: Float = 0.15f,
    val chamberingBudScale: Float = 1.08f,
    val chamberingClosedness: Float = 0.22f,
)
