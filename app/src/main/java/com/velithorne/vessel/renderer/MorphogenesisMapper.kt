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
        val gv = m.growthVisuals
        return GeneratedAnatomyParams(
            crownWidthMul = m.contour.crownWidthMul,
            thoraxWidthMul = m.contour.thoraxWidthMul,
            tailLengthMul = m.contour.tailLengthMul,
            asymmetryX = m.contour.asymmetryX,
            thermalBulge = m.contour.thermalBulge,
            visibleGrowthActivity = (m.visibleGrowthActivity * 0.45f + gv.activeAccretionPulse * 0.35f + gv.growthFrontEdgeIntensity * 0.2f).coerceIn(0f, 1f),
            metabolicPathwayMul = m.pathways.metabolicThickness,
            neuralPathwayMul = m.pathways.neuralThickness,
            signalBranchCount = m.pathways.signalBranchCount,
            tendonVisibilityMul = m.pathways.tendonVisibility,
            shellOpacityMul = m.tissue.shellOpacityMul,
            gelEnvelopeMul = m.tissue.gelEnvelopeMul,
            archiveLamellaDensityMul = m.tissue.archiveLamellaDensity,
            coolingVeilMul = m.tissue.coolingVeilStrength,
            seedFormBlend = seedBlend,
            tissueBodyFillMul = (m.bodyMass.totalOccupancy * 0.5f + gv.chamberFillVisual * 0.5f).coerceIn(0.15f, 1f),
            chamberInteriorMul = (chamberInterior * 0.55f + gv.chamberFillVisual * 0.45f).coerceIn(0f, 1f),
            growthFrontMul = (m.growthFront.activeIntensity * 0.45f + gv.growthFrontEdgeIntensity * 0.55f).coerceIn(0f, 1f),
            budSignalFrondMul = (m.budding.signalFrond * 0.45f + gv.frondBudLengthLeft * 0.55f).coerceIn(0f, 1f),
            budThermalVeilMul = (m.budding.thermalVeilSpine * 0.5f + gv.thermalVeilIntensity * 0.5f).coerceIn(0f, 1f),
            budArchiveLamellaMul = (m.budding.archiveLamella * 0.45f + gv.archiveDensityBands * 0.55f).coerceIn(0f, 1f),
            budNeuralCrownMul = (m.budding.neuralCrownBloom * 0.45f + gv.crownBloomIntensity * 0.55f).coerceIn(0f, 1f),
            organEmbedMul = embed.coerceIn(0.2f, 0.95f),
        )
    }
}
