package com.velithorne.vessel.branching

import com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage

data class BranchReadiness(
    /** 0..1 — visible leaning / UI. */
    val readiness: Float,
    val unlocked: Boolean,
) {
    companion object {
        fun compute(
            stage: SeedPodGrowthStage,
            maturityHigh: Float,
            tuning: BranchingTuning,
        ): BranchReadiness {
            val ord = stage.ordinal
            if (ord < tuning.branchReadinessUnlockStageOrdinal) {
                return BranchReadiness(0f, false)
            }
            val past = (ord - tuning.branchReadinessUnlockStageOrdinal).coerceAtLeast(0)
            val r = (0.2f + past * 0.12f + maturityHigh * 0.25f).coerceIn(0f, 1f)
            return BranchReadiness(r, true)
        }
    }
}
