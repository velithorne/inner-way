package com.velithorne.vessel.model

import com.velithorne.vessel.morphogenesis_core.CanonicalLifeEra
import com.velithorne.vessel.morphogenesis_core.GrowthCenter
import com.velithorne.vessel.morphogenesis_core.StructuralGraph
import com.velithorne.vessel.morphogenesis_core.TissueField

/**
 * Procedural self-assembly output driving the seed pod renderer.
 * [silhouettePolarMul] length 12 — radius multiplier per polar sample (generated silhouette, not uniform vesica).
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
    val silhouettePolarMul: FloatArray = FloatArray(12) { 1f },
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
        result = 31 * result + innerChamberOffsetNx.hashCode()
        result = 31 * result + innerChamberOffsetNy.hashCode()
        return result
    }
}
