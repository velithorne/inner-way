package com.velithorne.vessel.morphogenesis

/**
 * Morphogenesis rates and thresholds — single place for tuning.
 */
data class GrowthTuning(
    val pressureSmoothingAlpha: Float = 0.08f,
    val fieldDiffuseAlpha: Float = 0.06f,
    val genomeDriftAlpha: Float = 0.012f,
    val contourAdaptAlpha: Float = 0.045f,
    val organDriftAlpha: Float = 0.035f,
    val pathwayBranchThreshold: Float = 0.62f,
    val shellThickenThermal: Float = 0.55f,
    val archiveExpandLoad: Float = 0.48f,
    val lateralSpreadSignal: Float = 0.52f,
    val cranialExpandNeural: Float = 0.45f,
    val visibleGrowthPulseScale: Float = 0.85f,
    val explanationPressureFloor: Float = 0.28f,
)
