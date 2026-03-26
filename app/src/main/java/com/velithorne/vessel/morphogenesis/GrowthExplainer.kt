package com.velithorne.vessel.morphogenesis

/**
 * Deterministic explanations — lines gated by [GrowthVisualCues] so copy matches canvas.
 */
object GrowthExplainer {

    fun stageDisplayName(stage: GerminationStage): String = when (stage) {
        GerminationStage.DORMANT_SEED -> "Dormant Seed"
        GerminationStage.ACTIVATED_SEED -> "Activated Seed"
        GerminationStage.GERMINATING -> "Germinating"
        GerminationStage.CHAMBER_FORMATION -> "Chamber Formation"
        GerminationStage.BRANCHING -> "Branching"
        GerminationStage.RESERVOIR_DEEPENING -> "Reservoir Deepening"
        GerminationStage.SHELL_THICKENING -> "Shell Thickening"
        GerminationStage.STABILIZING -> "Stabilizing"
    }

    fun explain(
        acc: PressureAccumulator,
        genome: SpeciesGenome,
        field: GrowthPressureField,
        tuning: GrowthTuning,
        seedCore: SeedCore,
        chamberMass: ChamberMassModel,
        bodyMass: BodyMassFieldState,
        growthFront: GrowthFront,
        germinationStage: GerminationStage,
        gv: GrowthVisualCues,
    ): List<String> {
        val lines = mutableListOf<String>()

        when (germinationStage) {
            GerminationStage.DORMANT_SEED ->
                lines += "The crystalline seed lies latent; structure score is minimal."
            GerminationStage.ACTIVATED_SEED ->
                lines += "Latent channels charge; the seed core awakens before mass deposition."
            GerminationStage.GERMINATING ->
                lines += "Germination advances: tissue accretes around the central seed nucleus."
            GerminationStage.CHAMBER_FORMATION ->
                lines += "Internal chambers partition as the central metabolic field stabilizes."
            GerminationStage.BRANCHING ->
                if (gv.frondBudLengthLeft > tuning.textFrondBudThreshold || gv.frondBudLengthRight > tuning.textFrondBudThreshold) {
                    lines += "Lateral signal tissue buds; fronds extend under transport load."
                }
            GerminationStage.RESERVOIR_DEEPENING ->
                if (gv.lowerReservoirDepth > tuning.textReservoirThreshold) {
                    lines += "The lower archive basin deepens as storage burden accretes."
                }
            GerminationStage.SHELL_THICKENING ->
                if (gv.shellThickeningIntensity > tuning.textShellThickenThreshold) {
                    lines += "Shell bands thicken to route thermal stress along the perimeter veil."
                }
            GerminationStage.STABILIZING ->
                lines += "Accretion eases; coherence rises as recovery outpaces strain."
        }

        if (seedCore.germinationProgress > 0.35f && chamberMass.centralMetabolic > 0.32f && gv.chamberFillVisual > 0.25f) {
            lines += "Chamber mass gathers around the seed as the core unfolds."
        }
        if (bodyMass.totalOccupancy > 0.62f) {
            lines += "Body mass field merges seed influence with regional chambers."
        }
        if (growthFront.primaryType == GrowthFrontType.BRANCHING && gv.growthFrontEdgeIntensity > tuning.textFrondBudThreshold) {
            lines += "Growth front active along the lateral axis."
        }
        if (growthFront.primaryType == GrowthFrontType.COOLING_VEIL && gv.thermalVeilIntensity > tuning.textThermalVeilThreshold) {
            lines += "Cooling veil accretes on the shell where thermal load stays high."
        }

        if (acc.thermal > tuning.explanationPressureFloor && genome.coolingVeilBias > 0.35f && gv.thermalVeilIntensity > tuning.textThermalVeilThreshold) {
            lines += "Perimeter veil responds to thermal load; cooling channels prioritize shell routing."
        }
        if (acc.signal > tuning.textSignalBranchPressure && field.lateralSignal > 0.45f && gv.frondBudLengthLeft > tuning.textFrondBudThreshold) {
            lines += "Signal branches extend under sustained transport and metered channel strain."
        }
        if (acc.archive > tuning.explanationPressureFloor && gv.archiveDensityBands > tuning.textArchiveBandThreshold) {
            lines += "Archive basin widens as storage burden and structural strata accumulate."
        }
        if (acc.hunger > tuning.explanationPressureFloor && acc.reserve < 0.45f && gv.lowerReservoirDepth > tuning.textReservoirThreshold) {
            lines += "The lower reservoir thins under severe reserve deficit."
        }
        if (acc.neural > tuning.textNeuralCrownPressure && gv.crownBloomIntensity > tuning.textCrownBloomThreshold) {
            lines += "The cortical crown blooms under sustained neural throughput."
        }
        if (acc.motion > 0.4f) {
            lines += "Support tendons tense when motion and stabilization demand rise."
        }
        if (acc.recovery > 0.48f && acc.thermal < 0.35f) {
            lines += "Shell thickening eases when recovery tone rises and thermal stress falls."
        }
        if (lines.size < 2) {
            lines += "Morphogenesis stable; pressures within nominal growth envelope."
        }
        return lines.distinct().take(5)
    }

    fun activityLabel(
        acc: PressureAccumulator,
        visibleActivity: Float,
        gv: GrowthVisualCues,
        tuning: GrowthTuning,
    ): String = when {
        gv.frondBudLengthLeft > tuning.textFrondBudThreshold && visibleActivity > 0.4f -> "Branching"
        gv.frondBudLengthLeft > tuning.textFrondBudThreshold -> "Signal adapting"
        gv.thermalVeilIntensity > tuning.textThermalVeilThreshold -> "Cooling adaptation"
        gv.archiveDensityBands > tuning.textArchiveBandThreshold -> "Archive densifying"
        acc.recovery > 0.5f && acc.thermal < 0.35f -> "Stabilizing"
        else -> "Growing"
    }

    fun statusLine(
        acc: PressureAccumulator,
        stage: GerminationStage,
        growthFront: GrowthFront,
        chamber: ChamberMassModel,
        gv: GrowthVisualCues,
        tuning: GrowthTuning,
    ): String {
        val primary = when (growthFront.primaryType) {
            GrowthFrontType.BRANCHING -> if (gv.frondBudLengthLeft > tuning.textFrondBudThreshold) "Signal fronds budding" else null
            GrowthFrontType.THICKENING -> if (gv.shellThickeningIntensity > tuning.textShellThickenThreshold) "Shell bands accreting" else null
            GrowthFrontType.SWELLING -> if (gv.chamberFillVisual > 0.25f) "Chamber mass swelling" else null
            GrowthFrontType.EMBEDDING -> "Tissue embedding"
            GrowthFrontType.COOLING_VEIL -> if (gv.thermalVeilIntensity > tuning.textThermalVeilThreshold) "Cooling veil extending" else null
        }
        val regional = when {
            gv.crownBloomIntensity > tuning.textCrownBloomThreshold && acc.neural > 0.32f -> "Upper crown tissue forming"
            chamber.lateralSignal > 0.4f && gv.frondBudLengthLeft > tuning.textFrondBudThreshold -> "Lateral signal chambers broadening"
            chamber.lowerArchiveBasin > 0.42f && gv.archiveDensityBands > tuning.textArchiveBandThreshold -> "Archive basin densifying"
            acc.hunger > 0.52f && acc.reserve < 0.42f -> "Reservoir mass receding"
            else -> null
        }
        val parts = mutableListOf<String>()
        regional?.let { parts += it }
        primary?.let { parts += it }
        if (gv.thermalVeilIntensity > tuning.textThermalVeilThreshold * 0.85f) parts += "thermal veil engaged"
        if (acc.signal > 0.45f && regional == null && gv.frondBudLengthLeft > tuning.textFrondBudThreshold * 0.9f) parts += "lateral conduits active"
        if (parts.isEmpty()) return "Tissue accretion visible — seed chamber active."
        return parts.distinct().take(3).joinToString(" · ")
    }
}
