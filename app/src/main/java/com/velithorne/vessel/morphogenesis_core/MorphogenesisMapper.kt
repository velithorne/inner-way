package com.velithorne.vessel.morphogenesis_core

import com.velithorne.vessel.model.GeneratedAnatomyState
import com.velithorne.vessel.renderer_seedpod.ContourSampleSet
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

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
        val n = ContourSampleSet.forEra(era)
        val polar = buildPolarSilhouette(n, field, pressure, archetype, graph, hidden)
        val hard = buildHardEdgePreserve(n, field, pressure, graph)
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
            contourHardEdgePreserve = hard,
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
        n: Int,
        field: TissueField,
        pressure: GrowthPressureState,
        archetype: SeedArchetype,
        graph: StructuralGraph,
        hidden: InternalHiddenState,
    ): FloatArray {
        val out = FloatArray(n)
        val fr = graph.nodes.find { it.kind == MorphNodeKind.FROND_ROOT }
        val res = graph.nodes.find { it.kind == MorphNodeKind.RESERVE_BASIN_LOCUS }
        val crown = graph.nodes.find { it.kind == MorphNodeKind.CROWN_CHAMBER }
        val asym = archetype.asymmetryTolerance
        for (i in 0 until n) {
            val ang = (i / n.toFloat()) * (2f * PI.toFloat())
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

    private fun buildHardEdgePreserve(
        n: Int,
        field: TissueField,
        pressure: GrowthPressureState,
        graph: StructuralGraph,
    ): FloatArray {
        val h = FloatArray(n)
        val th = field.thermalTension.coerceIn(0f, 2f)
        val arch = field.archiveBurden.coerceIn(0f, 2f)
        for (i in 0 until n) {
            val ang = (i / n.toFloat()) * (2f * PI.toFloat())
            val ay = sin(ang)
            var v = 0f
            if (ay < -0.35f) v += th * 0.35f
            if (arch > 0.8f && abs(ang - PI.toFloat()) < 0.9f) v += arch * 0.12f
            h[i] = v.coerceIn(0f, 1f)
        }
        for (node in graph.nodes) {
            if (node.kind != MorphNodeKind.SCAR_ANCHOR && node.kind != MorphNodeKind.HEAT_MANTLE_RIDGE) continue
            val dx = (node.nx - 0.5f).toDouble()
            val dy = (node.ny - 0.5f).toDouble()
            val ang = atan2(dy, dx).toFloat()
            val idx = floor(((ang + PI.toFloat()) / (2f * PI.toFloat())) * n).toInt() % n
            val bump = node.strength * (if (node.kind == MorphNodeKind.SCAR_ANCHOR) 0.55f else 0.35f)
            h[idx] = (h[idx] + bump).coerceIn(0f, 1f)
            h[(idx + 1) % n] = (h[(idx + 1) % n] + bump * 0.5f).coerceIn(0f, 1f)
            h[(idx + n - 1) % n] = (h[(idx + n - 1) % n] + bump * 0.5f).coerceIn(0f, 1f)
        }
        return h
    }
}
