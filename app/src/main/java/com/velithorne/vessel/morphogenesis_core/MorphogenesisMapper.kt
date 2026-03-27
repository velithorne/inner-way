package com.velithorne.vessel.morphogenesis_core

import com.velithorne.vessel.model.GeneratedAnatomyState

object MorphogenesisMapper {

    fun toGeneratedAnatomy(
        graph: StructuralGraph,
        pressure: GrowthPressureState,
        hidden: InternalHiddenState,
        archetype: SeedArchetype,
        era: CanonicalLifeEra,
        growthCenters: List<GrowthCenter>,
    ): GeneratedAnatomyState {
        val field = TissueFieldEngine.sampleAt(0.5f, 0.48f, graph, pressure, hidden)
        val contour = IsoContourBuilder.build(field, archetype, era)
        return GeneratedAnatomyState(
            era = era,
            tissueCenter = field,
            shellRxMul = contour.shellRxMul,
            shellRyMul = contour.shellRyMul,
            innerRxMul = contour.innerRxMul,
            innerRyMul = contour.innerRyMul,
            verticalSkew = contour.verticalSkew,
            growthCenters = growthCenters,
            graph = graph,
            usingFieldContour = true,
        )
    }
}
