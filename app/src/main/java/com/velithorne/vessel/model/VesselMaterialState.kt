package com.velithorne.vessel.model

/**
 * Derived presentation parameters for membrane, pathways, and atmosphere (Phase 5).
 */
data class VesselMaterialState(
    val shellFillOpacity: Float,
    val shellEdgeAlpha: Float,
    val shellEdgeThicknessPx: Float,
    val innerHazeAlpha: Float,
    val organTranslucency: Float,
    val heatTintStrength: Float,
    val recoverySheenAlpha: Float,
    val sleepGlobalDim: Float,
    val vitalityCoherence: Float,
    val pathwayBaseAlpha: Float,
    val pathwayPulseSpeedMul: Float,
    val thermalEdgeBleed: Float,
    val thermalShimmerStrength: Float,
    val organHaloIntensity: Float,
    val archiveStrataContrast: Float,
    val selectionPeerDim: Float,
    val selectionFocusBoost: Float,
)
