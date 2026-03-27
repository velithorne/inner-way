package com.velithorne.vessel.model

import com.velithorne.vessel.morphogenesis_core.CanonicalLifeEra
import com.velithorne.vessel.morphogenesis_core.GrowthCenter
import com.velithorne.vessel.morphogenesis_core.StructuralGraph
import com.velithorne.vessel.morphogenesis_core.TissueField

/**
 * Procedural self-assembly output driving the seed pod renderer.
 */
data class GeneratedAnatomyState(
    val era: CanonicalLifeEra,
    val tissueCenter: TissueField,
    val shellRxMul: Float,
    val shellRyMul: Float,
    val innerRxMul: Float,
    val innerRyMul: Float,
    val verticalSkew: Float,
    val growthCenters: List<GrowthCenter>,
    val graph: StructuralGraph,
    val usingFieldContour: Boolean,
)
