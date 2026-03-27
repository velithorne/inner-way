package com.velithorne.vessel.progression

import com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage

object StageExplainer {

    fun stageTitle(stage: SeedPodGrowthStage): String = when (stage) {
        SeedPodGrowthStage.DORMANT_POD -> "Dormant pod"
        SeedPodGrowthStage.ACTIVATING_POD -> "Activating pod"
        SeedPodGrowthStage.GERMINATING_POD -> "Germinating pod"
        SeedPodGrowthStage.EARLY_BUDDING -> "Early budding"
        SeedPodGrowthStage.EARLY_CHAMBERING -> "Early chambering"
        SeedPodGrowthStage.CHAMBER_MATURED -> "Chamber matured"
        SeedPodGrowthStage.LINEAGE_DIFFERENTIATING -> "Lineage differentiating"
        SeedPodGrowthStage.FIRST_BRANCH_FORMING -> "First branch forming"
        SeedPodGrowthStage.ADAPTIVE_SHELL_VARIANT -> "Adaptive shell variant"
        SeedPodGrowthStage.SPECIALIZATION_READY -> "Specialization ready"
    }

    fun structuralNote(
        permanentStage: SeedPodGrowthStage,
        maturity: Float,
        paused: Boolean,
    ): String {
        if (paused) {
            return "Development toward ${stageTitle(nextStage(permanentStage))} is slowed while conditions are strained."
        }
        return "Crown and chamber structures remain established (maturity ${(maturity * 100f).toInt()}%)."
    }

    private fun nextStage(s: SeedPodGrowthStage): SeedPodGrowthStage {
        val o = s.ordinal + 1
        return SeedPodGrowthStage.entries.getOrNull(o) ?: SeedPodGrowthStage.SPECIALIZATION_READY
    }
}
