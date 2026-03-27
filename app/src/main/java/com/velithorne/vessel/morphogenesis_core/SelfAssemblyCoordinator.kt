package com.velithorne.vessel.morphogenesis_core

import com.velithorne.vessel.branching.DeviceProfile
import com.velithorne.vessel.config.SimulationMode
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage
import com.velithorne.vessel.physiology.PhysiologySnapshot
import com.velithorne.vessel.lineage.AdaptationMarker

/**
 * Single entry: archetype + pressure + graph + fields + biography → [SelfAssemblySnapshot].
 */
class SelfAssemblyCoordinator(
    private val specimenId: String,
    private val mode: SimulationMode,
) {
    private val tuning = MorphogenesisTuning.forMode(mode)
    private val pressureAcc = GrowthPressureAccumulator(
        alphaSlow = tuning.pressureAlphaSlow,
        alphaFast = tuning.pressureAlphaFast,
    )
    private var hidden = InternalHiddenState()
    private var biography = BiographyState(emptyList(), emptySet(), 0, 0)
    private var archetype: SeedArchetype = SeedArchetype.uniform()
    private var traits: HiddenSeedTraits = SeedArchetypeEngine.hiddenTraits(specimenId)

    fun ensureArchetype(phys: PhysiologySnapshot, device: DeviceProfile) {
        traits = SeedArchetypeEngine.hiddenTraits(specimenId)
        val pers = SeedArchetypeEngine.personalityFromAffinities(
            thermal = device.lowThermalHeadroom,
            neural = device.highSensorRichness,
            signal = device.highSignalDependency,
            reserve = device.storageDenseProfile,
        )
        archetype = SeedArchetypeEngine.derive(specimenId, phys.telemetry, device, traits, pers)
    }

    fun restorePressure(s: GrowthPressureState) = pressureAcc.restore(s)

    fun restoreBiography(b: BiographyState) {
        biography = b
    }

    fun restoreHidden(h: InternalHiddenState) {
        hidden = h
    }

    fun restorePersisted(p: MorphogenesisPersisted) {
        pressureAcc.restore(p.pressure)
        hidden = p.hidden
        biography = p.biography
    }

    fun step(
        phys: PhysiologySnapshot,
        stage: SeedPodGrowthStage,
        markers: List<AdaptationMarker>,
        device: DeviceProfile,
        nowMs: Long,
        timeSec: Float,
    ): SelfAssemblySnapshot {
        ensureArchetype(phys, device)
        val pressure = pressureAcc.step(phys)
        biography = BiographyEngine.step(biography, pressure, markers, nowMs)
        biography = BiographyEngine.maybeReroute(biography, pressure)
        hidden = InternalStateEngine.step(hidden, pressure, biography)
        val era = EraMapper.fromStage(stage)
        val graph = StructuralGraphEngine.build(
            specimenId = specimenId,
            archetype = archetype,
            pressure = pressure,
            hidden = hidden,
            stage = stage,
            scars = biography.scars,
            reroutes = biography.rerouteCount,
            era = era,
        )
        val centers = GrowthCenterEngine.fromGraph(graph, pressure, timeSec)
        val anatomy = MorphogenesisMapper.toGeneratedAnatomy(
            graph = graph,
            pressure = pressure,
            hidden = hidden,
            archetype = archetype,
            era = era,
            growthCenters = centers,
        )
        return SelfAssemblySnapshot(
            archetype = archetype,
            pressure = pressure,
            hidden = hidden,
            biography = biography,
            anatomy = anatomy,
        )
    }

    fun pressureSnapshot(): GrowthPressureState = pressureAcc.snapshot()
}
