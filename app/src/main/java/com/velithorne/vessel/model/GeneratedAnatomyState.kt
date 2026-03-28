package com.velithorne.vessel.model

import com.velithorne.vessel.morphogenesis_core.CanonicalLifeEra
import com.velithorne.vessel.morphogenesis_core.GrowthCenter
import com.velithorne.vessel.morphogenesis_core.StructuralGraph
import com.velithorne.vessel.morphogenesis_core.TissueField

/**
 * Procedural self-assembly output driving the seed pod renderer.
 * [silhouettePolarMul] — radius multiplier per polar sample (32–48 samples typical after smoothing).
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
    val silhouettePolarMul: FloatArray = FloatArray(40) { 1f },
    /** Per-sample 0..1 — preserve sharper local edges (plates, scars). */
    val contourHardEdgePreserve: FloatArray = FloatArray(40) { 0f },
    val innerChamberOffsetNx: Float = 0f,
    val innerChamberOffsetNy: Float = 0f,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as GeneratedAnatomyState
        if (era != other.era) return false
        if (tissueCenter != other.tissueCenter) return false
        if (shellRxMul != other.shellRxMul) return false
        if (shellRyMul != other.shellRyMul) return false
        if (innerRxMul != other.innerRxMul) return false
        if (innerRyMul != other.innerRyMul) return false
        if (verticalSkew != other.verticalSkew) return false
        if (growthCenters != other.growthCenters) return false
        if (graph != other.graph) return false
        if (usingFieldContour != other.usingFieldContour) return false
        if (!silhouettePolarMul.contentEquals(other.silhouettePolarMul)) return false
        if (!contourHardEdgePreserve.contentEquals(other.contourHardEdgePreserve)) return false
        if (innerChamberOffsetNx != other.innerChamberOffsetNx) return false
        if (innerChamberOffsetNy != other.innerChamberOffsetNy) return false
        return true
    }

    override fun hashCode(): Int {
        var result = era.hashCode()
        result = 31 * result + tissueCenter.hashCode()
        result = 31 * result + shellRxMul.hashCode()
        result = 31 * result + shellRyMul.hashCode()
        result = 31 * result + innerRxMul.hashCode()
        result = 31 * result + innerRyMul.hashCode()
        result = 31 * result + verticalSkew.hashCode()
        result = 31 * result + growthCenters.hashCode()
        result = 31 * result + graph.hashCode()
        result = 31 * result + usingFieldContour.hashCode()
        result = 31 * result + silhouettePolarMul.contentHashCode()
        result = 31 * result + contourHardEdgePreserve.contentHashCode()
        result = 31 * result + innerChamberOffsetNx.hashCode()
        result = 31 * result + innerChamberOffsetNy.hashCode()
        return result
    }
}
