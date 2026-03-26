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
    /** 0 = adult spindle, 1 = compact seed/embryo silhouette (morphogenesis baseline). */
    val seedFormBlend: Float,
    /** Body fill / internal mass presence (0..1). */
    val tissueBodyFillMul: Float,
    val chamberInteriorMul: Float,
    val growthFrontMul: Float,
    val budSignalFrondMul: Float,
    val budThermalVeilMul: Float,
    val budArchiveLamellaMul: Float,
    val budNeuralCrownMul: Float,
    /** Average organ tissue embedding (0..1). */
    val organEmbedMul: Float,
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
            seedFormBlend = 0.82f,
            tissueBodyFillMul = 0.45f,
            chamberInteriorMul = 0.5f,
            growthFrontMul = 0.35f,
            budSignalFrondMul = 0.25f,
            budThermalVeilMul = 0.22f,
            budArchiveLamellaMul = 0.28f,
            budNeuralCrownMul = 0.24f,
            organEmbedMul = 0.5f,
        )
    }
}
