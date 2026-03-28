package com.velithorne.vessel.genesis

import com.velithorne.vessel.branching.DeviceProfile
import com.velithorne.vessel.config.SimulationMode
import com.velithorne.vessel.morphogenesis_core.SeedArchetype
import com.velithorne.vessel.morphogenesis_core.StructuralGraph
import com.velithorne.vessel.physiology.PhysiologySnapshot
import kotlin.math.abs

/**
 * Classifies minimum viable body and composes genesis copy from graph + traits + pressures.
 */
object SeedGenesisEngine {

    fun build(
        specimenId: String,
        phys: PhysiologySnapshot,
        device: DeviceProfile,
        archetype: SeedArchetype,
        graph: StructuralGraph,
        birthPressure: BirthPressureState,
        mode: SimulationMode,
    ): SeedGenesisState {
        val traits = HiddenSeedTraits.fromSpecimenId(specimenId)
        val tuning = GenesisTuning.forMode(mode)
        val p = birthPressure.growthPressure
        val mvb = classifyMinimumViableBody(graph, archetype, p, traits, tuning)
        val driver = genesisDriver(mvb, archetype, p)
        val tendency = tendencyLine(mvb, archetype)
        return SeedGenesisState(
            traits = traits,
            birthPressure = birthPressure,
            minimumViableBody = mvb,
            archetype = archetype,
            graph = graph,
            genesisContourDriver = driver,
            birthTendencyLine = tendency,
        )
    }

    private fun classifyMinimumViableBody(
        graph: StructuralGraph,
        archetype: SeedArchetype,
        p: com.velithorne.vessel.morphogenesis_core.GrowthPressureState,
        traits: HiddenSeedTraits,
        tuning: GenesisTuning,
    ): MinimumViableBody {
        val reserve = graph.nodes.any { it.kind == com.velithorne.vessel.morphogenesis_core.MorphNodeKind.RESERVE_BASIN_LOCUS }
        val crown = graph.nodes.any { it.kind == com.velithorne.vessel.morphogenesis_core.MorphNodeKind.CROWN_CHAMBER }
        val fronds = graph.nodes.filter { it.kind == com.velithorne.vessel.morphogenesis_core.MorphNodeKind.FROND_ROOT }
        val asym = traits.latentAsymmetryBias * tuning.birthDivergenceMul
        val shellGate = GenesisGrammar.shellCoherenceGate(p, traits)

        return when {
            fronds.size == 1 && asym > 0.42f -> MinimumViableBody.SIGNAL_LATERAL_SINGLE
            asym > 0.55f -> MinimumViableBody.ASYMMETRY_FIRST
            !crown && reserve && shellGate < 0.35f -> MinimumViableBody.CORE_RESERVE_SPARK
            archetype.reserveBasinCompression > archetype.crownPotential + 0.08f && reserve -> MinimumViableBody.RESERVE_FIRST
            archetype.crownPotential > archetype.reserveBasinCompression + 0.1f && crown -> MinimumViableBody.CROWN_FIRST
            shellGate > 0.55f -> MinimumViableBody.SHELL_PRESSURE_FIRST
            else -> MinimumViableBody.MIXED_COHERENCE
        }
    }

    private fun genesisDriver(mvb: MinimumViableBody, archetype: SeedArchetype, p: com.velithorne.vessel.morphogenesis_core.GrowthPressureState): String {
        val base = when (mvb) {
            MinimumViableBody.CORE_RESERVE_SPARK -> "field-discovered contour (core + reserve spark)"
            MinimumViableBody.RESERVE_FIRST -> "reserve locus pressure"
            MinimumViableBody.CROWN_FIRST -> "crown lift potential"
            MinimumViableBody.SHELL_PRESSURE_FIRST -> "shell coherence nucleation"
            MinimumViableBody.SIGNAL_LATERAL_SINGLE -> "single lateral signal root"
            MinimumViableBody.ASYMMETRY_FIRST -> "latent asymmetry field"
            MinimumViableBody.MIXED_COHERENCE -> "multi-field coherence"
        }
        val ch = abs(archetype.asymmetryTolerance - 0.5f)
        return "$base · asym hint ${"%.2f".format(ch)} · sig ${"%.2f".format(p.signal)}"
    }

    private fun tendencyLine(mvb: MinimumViableBody, archetype: SeedArchetype): String =
        "${BirthExplainer.minimumViableLabel(mvb)} · crown ${"%.2f".format(archetype.crownPotential)} / reserve ${"%.2f".format(archetype.reserveBasinCompression)}"
}
