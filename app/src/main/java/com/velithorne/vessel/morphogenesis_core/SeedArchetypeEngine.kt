package com.velithorne.vessel.morphogenesis_core

import com.velithorne.vessel.branching.DeviceProfile
import com.velithorne.vessel.telemetry.TelemetrySnapshot
import kotlin.math.abs

object SeedArchetypeEngine {

    fun derive(
        specimenId: String,
        telem: TelemetrySnapshot,
        device: DeviceProfile,
        traits: HiddenSeedTraits,
        personality: LineagePersonality,
    ): SeedArchetype {
        val mem = (telem.memoryClassMb ?: 6).toFloat().coerceIn(2f, 24f)
        val storage = telem.storageUsedPct ?: 0.5f
        val sym = (0.5f + (1f - device.lowThermalHeadroom) * 0.15f + traits.symmetryBias * 0.1f).coerceIn(0.35f, 0.75f)
        val dens = (0.45f + storage * 0.25f + device.storageDenseProfile * 0.15f + traits.densityBias * 0.1f).coerceIn(0.3f, 0.85f)
        val reserveComp = (0.4f + device.batteryThermalTrend * 0.1f + storage * 0.2f + traits.reserveCompressionBias * 0.08f).coerceIn(0.25f, 0.9f)
        val crown = (0.42f + device.highSensorRichness * 0.2f + mem / 24f * 0.15f + traits.crownLiftBias * 0.06f).coerceIn(0.3f, 0.9f)
        val frond = (0.4f + device.highSignalDependency * 0.15f + traits.signalSpreadBias * 0.08f).coerceIn(0.3f, 0.9f)
        val shell = (0.45f + device.lowThermalHeadroom * 0.2f + traits.shellBias * 0.1f).coerceIn(0.3f, 0.9f)
        val archive = (0.38f + storage * 0.3f + device.storageDenseProfile * 0.2f).coerceIn(0.25f, 0.92f)
        val asym = (0.25f + abs(traits.latentAsymmetryBias - 0.5f) * 0.4f + device.highMotionLife * 0.15f).coerceIn(0.15f, 0.75f)
        val coh = (0.45f + traits.coherenceBias * 0.2f + personality.favorLateral * 0.05f).coerceIn(0.25f, 0.85f)
        val mut = (0.3f + device.nocturnalUsageBias * 0.1f + personality.mutationTolerance * 0.25f).coerceIn(0.2f, 0.8f)
        val pw = mutableListOf(
            personality.favorLateral.coerceIn(0f, 1f),
            personality.favorVertical.coerceIn(0f, 1f),
            personality.favorReserve.coerceIn(0f, 1f),
            personality.favorArchive.coerceIn(0f, 1f),
        )
        val sum = pw.sum().coerceAtLeast(1e-4f)
        for (i in pw.indices) pw[i] = pw[i] / sum
        return SeedArchetype(
            symmetryBias = sym,
            densityBias = dens,
            reserveBasinCompression = reserveComp,
            crownPotential = crown,
            frondPotential = frond,
            shellPlatingPotential = shell,
            archivePlatePotential = archive,
            asymmetryTolerance = asym,
            coherenceBias = coh,
            mutationTolerance = mut,
            personalityWeights = pw,
        )
    }

    fun hiddenTraits(specimenId: String): HiddenSeedTraits = HiddenSeedTraits.fromSpecimenId(specimenId)

    fun personalityFromAffinities(
        thermal: Float,
        neural: Float,
        signal: Float,
        reserve: Float,
    ): LineagePersonality = LineagePersonality(
        favorLateral = signal,
        favorVertical = neural,
        favorReserve = reserve,
        favorArchive = thermal * 0.5f + reserve * 0.3f,
        mutationTolerance = (signal + thermal) * 0.5f,
    )
}
