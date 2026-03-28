package com.velithorne.vessel.model

/**
 * Extended material response for depth pass (mirrors [SeedPodVisualState] with depth bias).
 */
data class SeedPodMaterialState(
    val shellTranslucency: Float,
    val shellThicknessNorm: Float,
    val edgeBrightness: Float,
    val innerHaze: Float,
    val thermalHaze: Float,
    val recoverySmoothing: Float,
    val hungerDim: Float,
)
