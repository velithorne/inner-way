package com.velithorne.vessel.renderer

/**
 * Procedural anatomy driving contour + tissue + pathway multipliers (from morphogenesis).
 */
data class GeneratedAnatomyParams(
    val crownWidthMul: Float,
    val thoraxWidthMul: Float,
    val tailLengthMul: Float,
    val asymmetryX: Float,
    val thermalBulge: Float,
    val visibleGrowthActivity: Float,
    val metabolicPathwayMul: Float,
    val neuralPathwayMul: Float,
    val signalBranchCount: Int,
    val tendonVisibilityMul: Float,
    val shellOpacityMul: Float,
    val gelEnvelopeMul: Float,
    val archiveLamellaDensityMul: Float,
    val coolingVeilMul: Float,
) {
    companion object {
        /** Identity until first morphogenesis tick. */
        val identity = GeneratedAnatomyParams(
            crownWidthMul = 1f,
            thoraxWidthMul = 1f,
            tailLengthMul = 1f,
            asymmetryX = 0f,
            thermalBulge = 0f,
            visibleGrowthActivity = 0f,
            metabolicPathwayMul = 1f,
            neuralPathwayMul = 1f,
            signalBranchCount = 2,
            tendonVisibilityMul = 1f,
            shellOpacityMul = 1f,
            gelEnvelopeMul = 1f,
            archiveLamellaDensityMul = 0.5f,
            coolingVeilMul = 0.4f,
        )
    }
}
