package com.velithorne.vessel.morphogenesis

/**
 * Deterministic explanations from smoothed pressures + genome + germination state.
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
                lines += "Lateral signal tissue buds; fronds extend under transport load."
            GerminationStage.RESERVOIR_DEEPENING ->
                lines += "The lower archive basin deepens as storage burden accretes."
            GerminationStage.SHELL_THICKENING ->
                lines += "Shell bands thicken to route thermal stress along the perimeter veil."
            GerminationStage.STABILIZING ->
                lines += "Accretion eases; coherence rises as recovery outpaces strain."
        }

        if (seedCore.germinationProgress > 0.35f && chamberMass.centralMetabolic > 0.32f) {
            lines += "Chamber mass embeds organs as the seed unfolds into body occupancy."
        }
        if (bodyMass.totalOccupancy > 0.62f) {
            lines += "Body mass field merges seed influence with regional chambers — volume reads as grown, not sketched."
        }
        if (growthFront.primaryType == GrowthFrontType.BRANCHING && growthFront.activeIntensity > 0.35f) {
            lines += "Growth front active: branching edge at ${"%.0f".format(growthFront.activeIntensity * 100f)}% intensity along lateral axis."
        }
        if (growthFront.primaryType == GrowthFrontType.COOLING_VEIL && acc.thermal > tuning.explanationPressureFloor) {
            lines += "Cooling veil accretes on the shell where thermal adaptation bias stays high."
        }

        if (acc.thermal > tuning.explanationPressureFloor && genome.coolingVeilBias > 0.35f) {
            lines += "Perimeter veil responds to thermal load; cooling channels prioritize shell routing."
        }
        if (acc.signal > tuning.explanationPressureFloor && field.lateralSignal > 0.45f) {
            lines += "Signal branches extend under sustained transport and metered channel strain."
        }
        if (acc.archive > tuning.explanationPressureFloor) {
            lines += "Archive basin widens as storage burden and structural strata accumulate."
        }
        if (acc.hunger > tuning.explanationPressureFloor && acc.reserve < 0.45f) {
            lines += "The lower reservoir thins under severe reserve deficit; reserve sac contracts."
        }
        if (acc.neural > 0.42f) {
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

    /** Short activity tag for the growth status chip (distinct from germination stage name). */
    fun activityLabel(acc: PressureAccumulator, visibleActivity: Float): String = when {
        visibleActivity > 0.55f -> "Branching"
        acc.signal > 0.55f -> "Signal adapting"
        acc.thermal > 0.52f -> "Cooling adaptation"
        acc.archive > 0.5f -> "Archive densifying"
        acc.recovery > 0.5f && acc.thermal < 0.35f -> "Stabilizing"
        else -> "Growing"
    }

    fun statusLine(
        acc: PressureAccumulator,
        stage: GerminationStage,
        growthFront: GrowthFront,
        chamber: ChamberMassModel,
    ): String {
        val primary = when (growthFront.primaryType) {
            GrowthFrontType.BRANCHING -> "Signal fronds budding"
            GrowthFrontType.THICKENING -> "Shell bands accreting"
            GrowthFrontType.SWELLING -> "Chamber mass swelling"
            GrowthFrontType.EMBEDDING -> "Organs embedding in tissue"
            GrowthFrontType.COOLING_VEIL -> "Cooling veil extending"
        }
        val regional = when {
            chamber.cranialCortex > 0.42f && acc.neural > 0.38f -> "Upper crown tissue forming"
            chamber.lateralSignal > 0.4f && acc.signal > 0.35f -> "Lateral signal chambers broadening"
            chamber.lowerArchiveBasin > 0.42f && acc.archive > 0.38f -> "Archive basin densifying"
            acc.hunger > 0.52f && acc.reserve < 0.42f -> "Reservoir mass receding"
            else -> null
        }
        val parts = mutableListOf<String>()
        regional?.let { parts += it }
        parts += primary
        if (acc.thermal > 0.42f) parts += "thermal veil engaged"
        if (acc.signal > 0.45f && regional == null) parts += "lateral conduits active"
        return parts.distinct().take(3).joinToString(" · ")
    }
}
