package com.velithorne.vessel.morphogenesis_core

import com.velithorne.vessel.model.GeneratedAnatomyState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object MorphogenesisMapper {

    private const val POLAR_SAMPLES = 12

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
        val polar = buildPolarSilhouette(field, pressure, archetype, graph, hidden)
        val (offNx, offNy) = innerChamberBias(graph, pressure)
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
            silhouettePolarMul = polar,
            innerChamberOffsetNx = offNx,
            innerChamberOffsetNy = offNy,
        )
    }

    private fun innerChamberBias(graph: StructuralGraph, pressure: GrowthPressureState): Pair<Float, Float> {
        val crown = graph.nodes.find { it.kind == MorphNodeKind.CROWN_CHAMBER }
        val arch = graph.nodes.find { it.kind == MorphNodeKind.ARCHIVE_PLATE_SEED }
        val nx = ((crown?.nx ?: 0.5f) - 0.5f) * 0.14f + pressure.signal * 0.04f
        val ny = ((arch?.ny ?: 0.55f) - 0.5f) * 0.1f + (pressure.archive - 0.5f) * 0.06f
        return nx.coerceIn(-0.12f, 0.12f) to ny.coerceIn(-0.1f, 0.1f)
    }

    private fun buildPolarSilhouette(
        field: TissueField,
        pressure: GrowthPressureState,
        archetype: SeedArchetype,
        graph: StructuralGraph,
        hidden: InternalHiddenState,
    ): FloatArray {
        val out = FloatArray(POLAR_SAMPLES)
        val fr = graph.nodes.find { it.kind == MorphNodeKind.FROND_ROOT }
        val res = graph.nodes.find { it.kind == MorphNodeKind.RESERVE_BASIN_LOCUS }
        val crown = graph.nodes.find { it.kind == MorphNodeKind.CROWN_CHAMBER }
        val asym = archetype.asymmetryTolerance
        for (i in 0 until POLAR_SAMPLES) {
            val ang = (i / POLAR_SAMPLES.toFloat()) * (2f * PI.toFloat())
            val ax = cos(ang)
            val ay = sin(ang)
            var m = 1f
            m += field.signal * 0.1f * sin(ang * 2f + pressure.signal)
            m += field.reserve * 0.08f * cos(ang * 2f + 0.4f)
            m += field.thermalTension * 0.07f * sin(ang + 1.1f)
            m += field.archiveBurden * 0.09f * cos(ang * 3f)
            m += ((fr?.nx ?: 0.5f) - 0.5f) * asym * 0.35f * sin(ang)
            m += ((crown?.ny ?: 0.2f) - 0.35f) * 0.2f * (if (ay < 0f) -ay else ay * 0.3f)
            m += ((res?.ny ?: 0.72f) - 0.5f) * 0.18f * (if (ay > 0f) ay else ay * 0.25f)
            m += hidden.lineageConfidence * 0.04f * sin(ang * 4f)
            m += pressure.motion * 0.06f * abs(sin(ang * 2f))
            out[i] = m.coerceIn(0.82f, 1.28f)
        }
        return out
    }
}
