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
        // Higher = compact seed vesica — **default product read is always seed** until growth load is extreme.
        val seedBlend = (0.97f - growthLoad * 0.35f).coerceIn(0.72f, 0.98f)
        val embed = (
            m.organEmbedding.metabolicHeart + m.organEmbedding.cortexCluster + m.organEmbedding.neuralGel +
                m.organEmbedding.archiveVault + m.organEmbedding.signalLungs + m.organEmbedding.vestibularMusc
            ) / 6f
        val chamberInterior = (
            m.chamberMass.cranialCortex * 0.22f + m.chamberMass.centralMetabolic * 0.28f +
                m.chamberMass.lateralSignal * 0.18f + m.chamberMass.lowerArchiveBasin * 0.2f +
                m.chamberMass.perimeterShell * 0.12f
            ).coerceIn(0f, 1f)
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
            tissueBodyFillMul = m.bodyMass.totalOccupancy.coerceIn(0.15f, 1f),
            chamberInteriorMul = chamberInterior,
            growthFrontMul = m.growthFront.activeIntensity.coerceIn(0f, 1f),
            budSignalFrondMul = m.budding.signalFrond,
            budThermalVeilMul = m.budding.thermalVeilSpine,
            budArchiveLamellaMul = m.budding.archiveLamella,
            budNeuralCrownMul = m.budding.neuralCrownBloom,
            organEmbedMul = embed.coerceIn(0.2f, 0.95f),
        )
    }
}
