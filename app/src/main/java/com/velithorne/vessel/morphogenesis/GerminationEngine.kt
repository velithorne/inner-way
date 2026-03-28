package com.velithorne.vessel.morphogenesis

import com.velithorne.vessel.util.Smoothing

object GerminationEngine {

    fun step(
        speciesSeed: SpeciesSeed,
        genome: SpeciesGenome,
        acc: PressureAccumulator,
        field: GrowthPressureField,
        prev: SeedCore?,
        tuning: GrowthTuning,
    ): SeedCore {
        val latent = (
            acc.signal * 0.22f + acc.neural * 0.2f + acc.thermal * 0.15f +
                field.cortical * 0.18f + speciesSeed.coherenceBias * 0.12f
            ).coerceIn(0f, 1f)

        val germTarget = (
            0.18f + acc.recovery * 0.22f + field.centralChamber * 0.28f + acc.attachment * 0.12f +
                (1f - acc.hunger) * 0.2f
            ).coerceIn(0f, 1f)

        val radiusTarget = (
            tuning.seedCoreRadiusMin +
                genome.resilienceBias * 0.02f +
                (1f - acc.reserve) * 0.035f +
                field.centralChamber * 0.025f
            ).coerceIn(tuning.seedCoreRadiusMin, tuning.seedCoreRadiusMax)

        val densityTarget = (0.42f + acc.archive * 0.18f + speciesSeed.densityBias * 0.25f + latent * 0.15f).coerceIn(0.2f, 1f)
        val reserveLum = (0.35f + (1f - acc.hunger) * 0.45f + acc.recovery * 0.25f).coerceIn(0.1f, 1f)
        val shellCoh = (0.4f + genome.shellThickness * 0.35f + field.perimeterShell * 0.25f).coerceIn(0.15f, 1f)
        val branchLatent = (latent * 0.55f + acc.signal * 0.25f + genome.antennaBranchBias * 0.2f).coerceIn(0f, 1f)
        val archiveLatent = (acc.archive * 0.45f + genome.archiveLamellaBias * 0.35f + field.lowerArchive * 0.2f).coerceIn(0f, 1f)
        val signalBias = (acc.signal * 0.5f + field.lateralSignal * 0.35f + genome.conduitDensityBias * 0.15f).coerceIn(0f, 1f)
        val thermalBias = (acc.thermal * 0.55f + genome.coolingVeilBias * 0.3f + field.perimeterShell * 0.15f).coerceIn(0f, 1f)
        val lattice = (acc.hunger * 0.35f + (1f - acc.reserve) * 0.3f + acc.thermal * 0.22f + (1f - acc.recovery) * 0.13f).coerceIn(0f, 1f)

        val raw = SeedCore(
            coreRadius = radiusTarget,
            seedDensity = densityTarget,
            reserveLuminance = reserveLum,
            shellCoherence = shellCoh,
            germinationProgress = germTarget,
            branchLatentEnergy = branchLatent,
            archiveLatentMass = archiveLatent,
            signalLatentBias = signalBias,
            thermalAdaptationBias = thermalBias,
            latticeStress = lattice,
        )
        if (prev == null) return raw
        val a = tuning.tissueAccretionAlpha
        val g = tuning.germinationProgressAlpha
        return SeedCore(
            coreRadius = Smoothing.lerp(prev.coreRadius, raw.coreRadius, a),
            seedDensity = Smoothing.lerp(prev.seedDensity, raw.seedDensity, a),
            reserveLuminance = Smoothing.lerp(prev.reserveLuminance, raw.reserveLuminance, a * 1.1f),
            shellCoherence = Smoothing.lerp(prev.shellCoherence, raw.shellCoherence, a),
            germinationProgress = Smoothing.lerp(prev.germinationProgress, raw.germinationProgress, g),
            branchLatentEnergy = Smoothing.lerp(prev.branchLatentEnergy, raw.branchLatentEnergy, a),
            archiveLatentMass = Smoothing.lerp(prev.archiveLatentMass, raw.archiveLatentMass, a),
            signalLatentBias = Smoothing.lerp(prev.signalLatentBias, raw.signalLatentBias, a),
            thermalAdaptationBias = Smoothing.lerp(prev.thermalAdaptationBias, raw.thermalAdaptationBias, a),
            latticeStress = Smoothing.lerp(prev.latticeStress, raw.latticeStress, a * 1.2f),
        )
    }
}
