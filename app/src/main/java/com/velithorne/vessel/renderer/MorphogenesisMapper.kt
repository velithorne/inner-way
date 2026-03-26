package com.velithorne.vessel.renderer

import com.velithorne.vessel.morphogenesis.MorphogenesisSnapshot

object MorphogenesisMapper {

    fun toGeneratedParams(m: MorphogenesisSnapshot): GeneratedAnatomyParams = GeneratedAnatomyParams(
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
    )
}
