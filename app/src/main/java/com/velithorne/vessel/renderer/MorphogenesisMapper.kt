package com.velithorne.vessel.renderer

import com.velithorne.vessel.morphogenesis.MorphogenesisSnapshot

object MorphogenesisMapper {

    fun toGeneratedParams(m: MorphogenesisSnapshot): GeneratedAnatomyParams {
        val growthLoad = (
            m.accumulated.archive * 0.28f +
                m.accumulated.signal * 0.24f +
                m.accumulated.neural * 0.18f +
                m.genome.shellThickness * 0.22f
            ).coerceIn(0f, 1f)
        val seedBlend = (0.88f - growthLoad * 0.62f).coerceIn(0.22f, 0.9f)
        return GeneratedAnatomyParams(
            crownWidthMul = m.contour.crownWidthMul,
            thoraxWidthMul = m.contour.thoraxWidthMul,
            tailLengthMul = m.contour.tailLengthMul,
            asymmetryX = m.contour.asymmetryX,
            thermalBulge = m.contour.thermalBulge,
            visibleGrowthActivity = m.visibleGrowthActivity,
            metabolicPathwayMul = m.pathways.metabolicThickness,
            neuralPathwayMul = m.pathways.neuralThickness,
            signalBranchCount = m.pathways.signalBranchCount,
            tendonVisibilityMul = m.pathways.tendonVisibility,
            shellOpacityMul = m.tissue.shellOpacityMul,
            gelEnvelopeMul = m.tissue.gelEnvelopeMul,
            archiveLamellaDensityMul = m.tissue.archiveLamellaDensity,
            coolingVeilMul = m.tissue.coolingVeilStrength,
            seedFormBlend = seedBlend,
        )
    }
}
