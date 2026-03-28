package com.velithorne.vessel.morphogenesis_core

import com.velithorne.vessel.genesis.GenesisGrammar
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage
import kotlin.random.Random

/**
 * Builds / updates structural graph from archetype, pressure, biography scars, era.
 */
object StructuralGraphEngine {

    fun build(
        specimenId: String,
        archetype: SeedArchetype,
        pressure: GrowthPressureState,
        hidden: InternalHiddenState,
        stage: SeedPodGrowthStage,
        scars: List<ScarPlate>,
        reroutes: Int,
        era: CanonicalLifeEra,
    ): StructuralGraph {
        val rnd = Random(specimenId.hashCode().toLong() xor (scars.size * 31L))
        val asym = archetype.asymmetryTolerance
        val nodes = mutableListOf<StructuralNode>()
        var id = 0
        fun nid() = "n_${specimenId.take(4)}_${id++}"

        nodes += StructuralNode(
            id = nid(),
            kind = MorphNodeKind.CORE_KNOT,
            ontologyClass = SpeciesOntology.CORE_KNOT,
            nx = 0.5f + (rnd.nextFloat() - 0.5f) * asym * 0.2f,
            ny = MorphologyGrammar.biasNodeY(MorphNodeKind.CORE_KNOT, pressure),
            strength = 0.85f + hidden.lineageConfidence * 0.15f,
            active = true,
        )
        val coreId = nodes.first().id

        if (era == CanonicalLifeEra.SEED) {
            val traits = HiddenSeedTraits.fromSpecimenId(specimenId)
            val resNx = MorphologyGrammar.biasNodeX(MorphNodeKind.RESERVE_BASIN_LOCUS, pressure, asym)
            val resNy = GenesisGrammar.reserveNyBias(pressure, traits)
            nodes += StructuralNode(
                id = nid(),
                kind = MorphNodeKind.RESERVE_BASIN_LOCUS,
                ontologyClass = SpeciesOntology.RESERVE_SAC_LATTICE,
                nx = resNx,
                ny = resNy,
                strength = (0.35f + pressure.reserve * 0.45f + traits.reserveCompressionBias * 0.12f).coerceIn(0.2f, 0.95f),
                active = true,
            )
            val crownGate = archetype.crownPotential * 0.45f + pressure.signal * 0.35f + traits.crownLiftBias * 0.15f
            if (crownGate > 0.38f) {
                val cnx = (0.5f + (rnd.nextFloat() - 0.5f) * asym * 0.35f).coerceIn(0.2f, 0.8f)
                nodes += StructuralNode(
                    id = nid(),
                    kind = MorphNodeKind.CROWN_CHAMBER,
                    ontologyClass = SpeciesOntology.PULSE_NODE,
                    nx = cnx,
                    ny = GenesisGrammar.crownNyBias(pressure, traits),
                    strength = (archetype.crownPotential * 0.35f + pressure.signal * 0.15f).coerceIn(0.15f, 0.65f),
                    active = true,
                )
            }
            val lateralGate = pressure.signal * 0.55f + archetype.frondPotential * 0.35f + traits.signalSpreadBias * 0.2f
            if (lateralGate > 0.42f) {
                val side = if (specimenId.hashCode() and 1 == 0) 1f else -1f
                nodes += StructuralNode(
                    id = nid(),
                    kind = MorphNodeKind.FROND_ROOT,
                    ontologyClass = SpeciesOntology.SIGNAL_FRONDS,
                    nx = GenesisGrammar.lateralSignalNx(pressure, traits, side),
                    ny = MorphologyGrammar.biasNodeY(MorphNodeKind.FROND_ROOT, pressure),
                    strength = (archetype.frondPotential * 0.4f + pressure.signal * 0.35f).coerceIn(0.18f, 0.75f),
                    active = true,
                )
            }
            nodes += StructuralNode(
                id = nid(),
                kind = MorphNodeKind.TRANSIENT_GROWTH_CENTER,
                ontologyClass = SpeciesOntology.OUTER_VEIL,
                nx = (0.5f + (rnd.nextFloat() - 0.5f) * 0.22f * (1f + asym)).coerceIn(0.2f, 0.8f),
                ny = (0.36f + traits.densityBias * 0.08f).coerceIn(0.22f, 0.52f),
                strength = (0.22f + hidden.growthPressure * 0.35f + traits.coherenceBias * 0.2f).coerceIn(0.15f, 0.7f),
                active = true,
            )
            nodes += StructuralNode(
                id = nid(),
                kind = MorphNodeKind.HEAT_MANTLE_RIDGE,
                ontologyClass = SpeciesOntology.HEAT_MANTLE,
                nx = (0.48f + pressure.thermal * 0.06f - asym * 0.04f).coerceIn(0.15f, 0.85f),
                ny = (0.4f + pressure.thermal * 0.1f).coerceIn(0.25f, 0.62f),
                strength = (0.25f + pressure.thermal * 0.35f + traits.shellBias * 0.15f).coerceIn(0.18f, 0.72f),
                active = true,
            )
        }

        if (era != CanonicalLifeEra.SEED && stage.ordinal >= SeedPodGrowthStage.GERMINATING_POD.ordinal) {
            nodes += StructuralNode(
                id = nid(),
                kind = MorphNodeKind.RESERVE_BASIN_LOCUS,
                ontologyClass = SpeciesOntology.RESERVE_SAC_LATTICE,
                nx = MorphologyGrammar.biasNodeX(MorphNodeKind.RESERVE_BASIN_LOCUS, pressure, asym),
                ny = MorphologyGrammar.biasNodeY(MorphNodeKind.RESERVE_BASIN_LOCUS, pressure),
                strength = 0.5f + pressure.reserve * 0.4f,
                active = true,
            )
        }
        if (era != CanonicalLifeEra.SEED && stage.ordinal >= SeedPodGrowthStage.EARLY_BUDDING.ordinal) {
            nodes += StructuralNode(
                id = nid(),
                kind = MorphNodeKind.CROWN_CHAMBER,
                ontologyClass = SpeciesOntology.PULSE_NODE,
                nx = 0.5f,
                ny = MorphologyGrammar.biasNodeY(MorphNodeKind.CROWN_CHAMBER, pressure),
                strength = archetype.crownPotential * 0.6f + pressure.signal * 0.2f,
                active = true,
            )
        }
        if (era != CanonicalLifeEra.SEED && stage.ordinal >= SeedPodGrowthStage.EARLY_CHAMBERING.ordinal) {
            nodes += StructuralNode(
                id = nid(),
                kind = MorphNodeKind.FROND_ROOT,
                ontologyClass = SpeciesOntology.SIGNAL_FRONDS,
                nx = MorphologyGrammar.biasNodeX(MorphNodeKind.FROND_ROOT, pressure, asym),
                ny = MorphologyGrammar.biasNodeY(MorphNodeKind.FROND_ROOT, pressure),
                strength = archetype.frondPotential * 0.55f + pressure.signal * 0.35f,
                active = true,
            )
            nodes += StructuralNode(
                id = nid(),
                kind = MorphNodeKind.HEAT_MANTLE_RIDGE,
                ontologyClass = SpeciesOntology.HEAT_MANTLE,
                nx = 0.5f - pressure.signal * 0.05f,
                ny = MorphologyGrammar.biasNodeY(MorphNodeKind.HEAT_MANTLE_RIDGE, pressure),
                strength = 0.4f + pressure.thermal * 0.45f,
                active = true,
            )
        }
        if (stage.ordinal >= SeedPodGrowthStage.CHAMBER_MATURED.ordinal) {
            nodes += StructuralNode(
                id = nid(),
                kind = MorphNodeKind.ARCHIVE_PLATE_SEED,
                ontologyClass = SpeciesOntology.ARCHIVE_PLATES,
                nx = 0.5f,
                ny = 0.62f + pressure.archive * 0.08f,
                strength = archetype.archivePlatePotential * 0.5f + pressure.archive * 0.4f,
                active = true,
            )
            nodes += StructuralNode(
                id = nid(),
                kind = MorphNodeKind.BRACE_ANCHOR,
                ontologyClass = SpeciesOntology.SUSPENSION_FIELD,
                nx = 0.5f,
                ny = 0.48f,
                strength = 0.35f + pressure.motion * 0.4f,
                active = true,
            )
        }
        if (era.ordinal >= CanonicalLifeEra.BRANCHING_THRESHOLD.ordinal) {
            nodes += StructuralNode(
                id = nid(),
                kind = MorphNodeKind.TRANSIENT_GROWTH_CENTER,
                ontologyClass = SpeciesOntology.OUTER_VEIL,
                nx = 0.5f + (rnd.nextFloat() - 0.5f) * 0.25f,
                ny = 0.4f,
                strength = hidden.growthPressure.coerceIn(0.2f, 1f),
                active = true,
            )
        }
        for (sc in scars) {
            nodes += StructuralNode(
                id = nid(),
                kind = MorphNodeKind.SCAR_ANCHOR,
                ontologyClass = SpeciesOntology.OUTER_VEIL,
                nx = sc.nx,
                ny = sc.ny,
                strength = sc.strength,
                active = true,
            )
        }
        if (reroutes > 0) {
            nodes += StructuralNode(
                id = nid(),
                kind = MorphNodeKind.REROUTE_JUNCTION,
                ontologyClass = SpeciesOntology.SIGNAL_FRONDS,
                nx = 0.48f,
                ny = 0.52f,
                strength = (0.3f + reroutes * 0.08f).coerceAtMost(1f),
                active = true,
            )
        }

        val edges = mutableListOf<StructuralEdge>()
        var eid = 0
        fun eid() = "e_${eid++}"
        val reserveNodes = nodes.filter { it.kind == MorphNodeKind.RESERVE_BASIN_LOCUS }
        val frond = nodes.find { it.kind == MorphNodeKind.FROND_ROOT }
        if (reserveNodes.isNotEmpty()) {
            edges += StructuralEdge(eid(), MorphEdgeKind.RESERVE_CONDUIT, coreId, reserveNodes.first().id, 0.7f)
        }
        if (frond != null) {
            edges += StructuralEdge(eid(), MorphEdgeKind.SIGNAL_CONDUIT, coreId, frond.id, 0.65f)
        }
        val brace = nodes.find { it.kind == MorphNodeKind.BRACE_ANCHOR }
        if (brace != null) {
            edges += StructuralEdge(eid(), MorphEdgeKind.SUPPORT_BRACE, coreId, brace.id, 0.55f)
        }
        if (reroutes > 0) {
            val rj = nodes.find { it.kind == MorphNodeKind.REROUTE_JUNCTION }
            if (rj != null && frond != null) {
                edges += StructuralEdge(eid(), MorphEdgeKind.REPAIR_REROUTE, frond.id, rj.id, 0.5f)
            }
        }
        return StructuralGraph(nodes, edges)
    }
}
