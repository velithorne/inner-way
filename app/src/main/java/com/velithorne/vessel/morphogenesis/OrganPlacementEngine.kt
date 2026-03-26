package com.velithorne.vessel.morphogenesis

import com.velithorne.vessel.physiology.OrganType
import com.velithorne.vessel.physiology.SpeciesState
import com.velithorne.vessel.util.Smoothing
import kotlin.math.abs

/**
 * Derives normalized organ anchors from seed, genome, and pressure field.
 */
object OrganPlacementEngine {

    fun buildGraph(
        seed: SpeciesSeed,
        genome: SpeciesGenome,
        field: GrowthPressureField,
        species: SpeciesState,
        prev: StructuralGraph?,
        tuning: GrowthTuning,
    ): StructuralGraph {
        val asym = genome.asymmetryBias * seed.symmetryBias.let { 1f - abs(it - 0.5f) * 0.4f }
        val lungSpread = 0.33f + field.lateralSignal * 0.12f + genome.antennaBranchBias * 0.08f
        val heartY = 0.58f + genome.lowerReservoirBias * 0.06f - species.hunger * 0.04f
        val cortexY = 0.28f - genome.cranialExpansionBias * 0.06f - species.neuralActivity * 0.02f
        val archiveY = 0.74f + genome.lowerReservoirBias * 0.05f + field.lowerArchive * 0.04f

        fun smooth(id: String, ox: Float, oy: Float, r: Float, type: OrganType?, kind: StructuralNodeKind): StructuralNode {
            val old = prev?.nodes?.find { it.id == id }
            val nx = if (old != null) Smoothing.lerp(old.nx, ox, tuning.organDriftAlpha) else ox
            val ny = if (old != null) Smoothing.lerp(old.ny, oy, tuning.organDriftAlpha) else oy
            val nr = if (old != null) Smoothing.lerp(old.influenceRadius, r, tuning.organDriftAlpha) else r
            return StructuralNode(id, kind, type, nx, ny, nr, mass = r * 1.2f)
        }

        val nodes = mutableListOf(
            smooth("heart", 0.5f + asym * 0.02f, heartY, 0.072f, OrganType.METABOLIC_HEART, StructuralNodeKind.ORGAN),
            smooth("cortex", 0.5f - asym * 0.015f, cortexY, 0.095f, OrganType.CORTEX_CLUSTER, StructuralNodeKind.ORGAN),
            smooth("gel", 0.5f, cortexY + 0.07f, 0.125f, OrganType.NEURAL_GEL, StructuralNodeKind.ORGAN),
            smooth("vault", 0.5f, archiveY, 0.115f, OrganType.ARCHIVE_VAULT, StructuralNodeKind.ORGAN),
            smooth("lung_l", 0.5f - lungSpread, 0.47f + field.lateralSignal * 0.02f, 0.068f, OrganType.SIGNAL_LUNGS, StructuralNodeKind.ORGAN),
            smooth("lung_r", 0.5f + lungSpread, 0.47f + field.lateralSignal * 0.02f, 0.068f, OrganType.SIGNAL_LUNGS, StructuralNodeKind.ORGAN),
            smooth("vest", 0.5f, 0.51f, 0.14f, OrganType.VESTIBULAR_MUSCULATURE, StructuralNodeKind.ORGAN),
            smooth("thermal", 0.5f, 0.48f, 0.48f, OrganType.THERMAL_MEMBRANE, StructuralNodeKind.ORGAN),
            StructuralNode("shell_n", StructuralNodeKind.SHELL_CONTROL, null, 0.5f, 0.12f, 0.2f, 0.3f),
            StructuralNode("shell_s", StructuralNodeKind.SHELL_CONTROL, null, 0.5f, 0.88f, 0.22f, 0.35f),
        )

        val byId = nodes.associateBy { it.id }
        fun edge(id: String, a: String, b: String, k: StructuralEdgeKind, s: Float) =
            StructuralEdge(id, a, b, k, s)

        val edges = listOf(
            edge("e_hc", "heart", "cortex", StructuralEdgeKind.METABOLIC_CONDUIT, 0.55f + field.centralChamber * 0.2f),
            edge("e_hv", "heart", "vault", StructuralEdgeKind.METABOLIC_CONDUIT, 0.5f + field.lowerArchive * 0.25f),
            edge("e_cg", "cortex", "gel", StructuralEdgeKind.NEURAL_PATH, 0.6f + field.cortical * 0.2f),
            edge("e_cl", "cortex", "lung_l", StructuralEdgeKind.NEURAL_PATH, 0.45f + field.lateralSignal * 0.25f),
            edge("e_cr", "cortex", "lung_r", StructuralEdgeKind.NEURAL_PATH, 0.45f + field.lateralSignal * 0.25f),
            edge("e_lv", "lung_l", "vault", StructuralEdgeKind.SIGNAL_BRANCH, 0.35f + genome.conduitDensityBias * 0.2f),
            edge("e_rv", "lung_r", "vault", StructuralEdgeKind.SIGNAL_BRANCH, 0.35f + genome.conduitDensityBias * 0.2f),
            edge("e_v1", "vest", "shell_n", StructuralEdgeKind.SUPPORT_TENDON, genome.tendonDensityBias),
            edge("e_v2", "vest", "shell_s", StructuralEdgeKind.SUPPORT_TENDON, genome.tendonDensityBias * 0.9f),
        ).filter { byId[it.fromId] != null && byId[it.toId] != null }

        return StructuralGraph(nodes, edges)
    }
}
