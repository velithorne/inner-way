package com.velithorne.vessel.morphogenesis

object GrowthStageClassifier {

    fun classify(
        seed: SeedCore,
        chamber: ChamberMassModel,
        field: GrowthPressureField,
        acc: PressureAccumulator,
        genome: SpeciesGenome,
        bodyMass: BodyMassFieldState,
    ): GerminationStage {
        val structureScore = (
            chamber.cranialCortex + chamber.centralMetabolic + chamber.lateralSignal +
                chamber.lowerArchiveBasin + chamber.perimeterShell
            ) / 5f

        val latent = seed.branchLatentEnergy * 0.35f + seed.archiveLatentMass * 0.25f + seed.signalLatentBias * 0.2f +
            seed.thermalAdaptationBias * 0.2f

        return when {
            structureScore < 0.18f && seed.germinationProgress < 0.22f -> GerminationStage.DORMANT_SEED
            structureScore < 0.28f && latent > 0.42f -> GerminationStage.ACTIVATED_SEED
            structureScore < 0.42f && seed.germinationProgress in 0.2f..0.55f -> GerminationStage.GERMINATING
            field.centralChamber > 0.48f && chamber.centralMetabolic > 0.38f -> GerminationStage.CHAMBER_FORMATION
            field.lateralSignal > 0.52f && chamber.lateralSignal > acc.archive * 0.9f -> GerminationStage.BRANCHING
            field.lowerArchive > 0.5f && chamber.lowerArchiveBasin > 0.42f -> GerminationStage.RESERVOIR_DEEPENING
            field.perimeterShell > 0.52f && genome.shellThickness > 0.55f -> GerminationStage.SHELL_THICKENING
            acc.recovery > 0.48f && acc.thermal < 0.38f && bodyMass.totalOccupancy > 0.55f -> GerminationStage.STABILIZING
            acc.signal > 0.48f -> GerminationStage.BRANCHING
            acc.archive > 0.45f -> GerminationStage.RESERVOIR_DEEPENING
            acc.thermal > 0.45f -> GerminationStage.SHELL_THICKENING
            else -> GerminationStage.GERMINATING
        }
    }
}
