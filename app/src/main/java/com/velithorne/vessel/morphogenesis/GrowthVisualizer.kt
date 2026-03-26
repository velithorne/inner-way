package com.velithorne.vessel.morphogenesis

/**
 * Maps morphogenesis state → explicit renderable growth intensities (canvas + text gating).
 */
data class GrowthVisualCues(
    val crownBloomIntensity: Float,
    val frondBudLengthLeft: Float,
    val frondBudLengthRight: Float,
    val lowerReservoirDepth: Float,
    val shellThickeningIntensity: Float,
    val archiveDensityBands: Float,
    val thermalVeilIntensity: Float,
    val growthFrontEdgeIntensity: Float,
    val activeAccretionPulse: Float,
    val stageVisualBias: Float,
    val chamberFillVisual: Float,
)

object GrowthVisualizer {

    fun compute(
        acc: PressureAccumulator,
        field: GrowthPressureField,
        genome: SpeciesGenome,
        chamberMass: ChamberMassModel,
        bodyMass: BodyMassFieldState,
        budding: BuddingStructure,
        growthFront: GrowthFront,
        seedCore: SeedCore,
        germinationStage: GerminationStage,
        visibleGrowthActivity: Float,
    ): GrowthVisualCues {
        val crownBloom = (
            chamberMass.cranialCortex * 0.45f + acc.neural * 0.35f + field.cortical * 0.25f + budding.neuralCrownBloom * 0.35f
            ).coerceIn(0f, 1f)

        val frondBase = (
            chamberMass.lateralSignal * 0.4f + acc.signal * 0.35f + field.lateralSignal * 0.25f + budding.signalFrond * 0.45f
            ).coerceIn(0f, 1f)
        val asym = genome.asymmetryBias.coerceIn(0f, 0.45f)
        val frondL = (frondBase * (1f + asym * 0.15f)).coerceIn(0f, 1f)
        val frondR = (frondBase * (1f - asym * 0.12f)).coerceIn(0f, 1f)

        val lowerDepth = (
            chamberMass.lowerArchiveBasin * 0.45f + acc.archive * 0.35f + field.lowerArchive * 0.25f + budding.archiveLamella * 0.35f
            ).coerceIn(0f, 1f)

        val shellThick = (
            field.perimeterShell * 0.45f + acc.thermal * 0.35f + genome.shellThickness * 0.25f + budding.thermalVeilSpine * 0.3f
            ).coerceIn(0f, 1f)

        val archiveBands = (
            chamberMass.lowerArchiveBasin * 0.35f + genome.archiveLamellaBias * 0.3f + acc.archive * 0.35f
            ).coerceIn(0f, 1f)

        val thermalVeil = (
            acc.thermal * 0.45f + field.perimeterShell * 0.3f + budding.thermalVeilSpine * 0.4f + genome.coolingVeilBias * 0.2f
            ).coerceIn(0f, 1f)

        val frontEdge = (growthFront.activeIntensity * 0.55f + visibleGrowthActivity * 0.45f).coerceIn(0f, 1f)

        val accretionPulse = (
            seedCore.germinationProgress * 0.35f + visibleGrowthActivity * 0.35f + chamberMass.centralMetabolic * 0.3f
            ).coerceIn(0f, 1f)

        val stageBias = when (germinationStage) {
            GerminationStage.DORMANT_SEED, GerminationStage.ACTIVATED_SEED -> 0.15f
            GerminationStage.GERMINATING -> 0.45f
            GerminationStage.CHAMBER_FORMATION -> 0.55f
            GerminationStage.BRANCHING -> 0.72f
            GerminationStage.RESERVOIR_DEEPENING -> 0.68f
            GerminationStage.SHELL_THICKENING -> 0.75f
            GerminationStage.STABILIZING -> 0.5f
        }

        val chamberFill = (
            bodyMass.totalOccupancy * 0.45f + chamberMass.centralMetabolic * 0.25f + chamberMass.cranialCortex * 0.15f + chamberMass.lowerArchiveBasin * 0.15f
            ).coerceIn(0f, 1f)

        return GrowthVisualCues(
            crownBloomIntensity = crownBloom,
            frondBudLengthLeft = frondL,
            frondBudLengthRight = frondR,
            lowerReservoirDepth = lowerDepth,
            shellThickeningIntensity = shellThick,
            archiveDensityBands = archiveBands,
            thermalVeilIntensity = thermalVeil,
            growthFrontEdgeIntensity = frontEdge,
            activeAccretionPulse = accretionPulse,
            stageVisualBias = stageBias,
            chamberFillVisual = chamberFill,
        )
    }
}
