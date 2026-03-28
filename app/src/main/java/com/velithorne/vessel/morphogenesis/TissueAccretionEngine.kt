package com.velithorne.vessel.morphogenesis

import com.velithorne.vessel.util.Smoothing
import kotlin.math.hypot

object TissueAccretionEngine {

    fun stepChamber(
        genome: SpeciesGenome,
        field: GrowthPressureField,
        acc: PressureAccumulator,
        seed: SeedCore,
        prev: ChamberMassModel?,
        tuning: GrowthTuning,
    ): ChamberMassModel {
        val scale = tuning.chamberFillDensityScale
        val raw = ChamberMassModel(
            cranialCortex = (
                field.cortical * 0.55f + acc.neural * 0.25f + genome.cranialExpansionBias * 0.2f
                ).coerceIn(0f, 1f) * scale,
            centralMetabolic = (
                field.centralChamber * 0.45f + acc.reserve * 0.28f + seed.germinationProgress * 0.22f + acc.recovery * 0.15f
                ).coerceIn(0f, 1f) * scale,
            lateralSignal = (
                field.lateralSignal * 0.5f + acc.signal * 0.35f + genome.antennaBranchBias * 0.15f
                ).coerceIn(0f, 1f) * scale,
            lowerArchiveBasin = (
                field.lowerArchive * 0.55f + acc.archive * 0.3f + genome.lowerReservoirBias * 0.15f
                ).coerceIn(0f, 1f) * scale,
            perimeterShell = (
                field.perimeterShell * 0.5f + acc.thermal * 0.25f + genome.shellThickness * 0.25f
                ).coerceIn(0f, 1f) * scale,
        )
        if (prev == null) return raw
        val a = tuning.tissueAccretionAlpha
        return ChamberMassModel(
            cranialCortex = Smoothing.lerp(prev.cranialCortex, raw.cranialCortex, a),
            centralMetabolic = Smoothing.lerp(prev.centralMetabolic, raw.centralMetabolic, a),
            lateralSignal = Smoothing.lerp(prev.lateralSignal, raw.lateralSignal, a),
            lowerArchiveBasin = Smoothing.lerp(prev.lowerArchiveBasin, raw.lowerArchiveBasin, a),
            perimeterShell = Smoothing.lerp(prev.perimeterShell, raw.perimeterShell, a),
        )
    }

    fun computeBudding(
        acc: PressureAccumulator,
        field: GrowthPressureField,
        genome: SpeciesGenome,
        seed: SeedCore,
        tuning: GrowthTuning,
    ): BuddingStructure {
        val th = (acc.thermal * tuning.shellThickenRate * 12f + genome.coolingVeilBias * 0.35f).coerceIn(0f, 1f)
        val ar = (acc.archive * tuning.archiveLamellaDensityRate * 14f + genome.archiveLamellaBias * 0.4f).coerceIn(0f, 1f)
        val crown = (acc.neural * tuning.crownBloomRate * 14f + field.cortical * 0.35f).coerceIn(0f, 1f)
        val frond = (acc.signal * tuning.frondExtensionRate * 12f + field.lateralSignal * 0.38f).coerceIn(0f, 1f)
        val reserve = (acc.hunger * tuning.reserveSacContractionRate * 10f + (1f - seed.reserveLuminance) * 0.25f).coerceIn(0f, 1f)
        return BuddingStructure(
            signalFrond = if (frond > tuning.buddingThreshold) frond else frond * 0.65f,
            thermalVeilSpine = if (th > tuning.buddingThreshold) th else th * 0.6f,
            archiveLamella = if (ar > tuning.buddingThreshold) ar else ar * 0.62f,
            neuralCrownBloom = if (crown > tuning.buddingThreshold) crown else crown * 0.58f,
            reserveSac = reserve,
        )
    }

    fun computeGrowthFront(
        acc: PressureAccumulator,
        field: GrowthPressureField,
        buds: BuddingStructure,
        visibleActivity: Float,
        tuning: GrowthTuning,
    ): GrowthFront {
        val primary = when {
            buds.signalFrond >= buds.thermalVeilSpine && buds.signalFrond >= buds.archiveLamella -> GrowthFrontType.BRANCHING
            buds.thermalVeilSpine >= buds.archiveLamella -> GrowthFrontType.COOLING_VEIL
            buds.archiveLamella >= buds.neuralCrownBloom -> GrowthFrontType.THICKENING
            else -> GrowthFrontType.SWELLING
        }
        val secondary = when {
            field.centralChamber > 0.52f -> GrowthFrontType.EMBEDDING
            acc.neural > 0.48f -> GrowthFrontType.SWELLING
            else -> GrowthFrontType.THICKENING
        }
        val dirX = (acc.signal - 0.5f) * 1.4f + field.lateralSignal * 0.25f
        val dirY = (acc.archive - acc.hunger) * 0.9f - field.lowerArchive * 0.2f
        val len = hypot(dirX.toDouble(), dirY.toDouble()).toFloat().coerceAtLeast(0.05f)
        val intensity = (
            visibleActivity * 0.45f +
                buds.signalFrond * 0.18f + buds.thermalVeilSpine * 0.15f + buds.archiveLamella * 0.12f +
                field.perimeterShell * 0.1f
            ).coerceIn(0f, 1f) * tuning.growthFrontIntensityScale
        return GrowthFront(
            activeIntensity = intensity,
            directionX = (dirX / len).coerceIn(-1f, 1f),
            directionY = (dirY / len).coerceIn(-1f, 1f),
            primaryType = primary,
            secondaryType = secondary,
        )
    }
}
