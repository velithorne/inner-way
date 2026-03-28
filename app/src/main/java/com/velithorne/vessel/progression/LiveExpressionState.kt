package com.velithorne.vessel.progression

/**
 * Reversible presentation layer — can dip with stress/fever without affecting [StructuralGrowthState].
 */
data class LiveExpressionState(
    val vitalityBrightnessMul: Float,
    val feverTintMul: Float,
    val stressContractionMul: Float,
    val reserveDimMul: Float,
    val thermalAgitationMul: Float,
    val shellCoherenceMul: Float,
    val crownGlowMul: Float,
    val lateralInflationMul: Float,
    val stressShimmerMul: Float,
    /** Short label for UI: Calm / Strained / etc. */
    val conditionLabel: String,
)
