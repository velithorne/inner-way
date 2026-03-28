package com.velithorne.vessel.branching

import com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage

object BranchSelectionEngine {

    fun step(
        prev: MorphologyBranchState,
        target: BranchAffinity,
        stage: SeedPodGrowthStage,
        maturityHigh: Float,
        dtSec: Float,
        tuning: BranchingTuning,
    ): MorphologyBranchState {
        val lerp = (tuning.affinityLerpPerSec * dtSec).coerceIn(0.001f, 0.08f)
        fun lerpToward(c: Float, t: Float): Float {
            val d = (t - c) * lerp
            return (c + d.coerceIn(-tuning.affinityDeltaCap, tuning.affinityDeltaCap)).coerceIn(0.01f, 1f)
        }
        val a = prev.affinities
        var next = BranchAffinity(
            thermalShell = lerpToward(a.thermalShell, target.thermalShell),
            signalFrond = lerpToward(a.signalFrond, target.signalFrond),
            crownNeural = lerpToward(a.crownNeural, target.crownNeural),
            reserveBasin = lerpToward(a.reserveBasin, target.reserveBasin),
            archiveCore = lerpToward(a.archiveCore, target.archiveCore),
            motionBraced = lerpToward(a.motionBraced, target.motionBraced),
            balanced = lerpToward(a.balanced, target.balanced),
        ).normalized()

        val pairs = LineageBranch.entries.map { b -> b to next[b] }
        val sorted = pairs.sortedByDescending { it.second }
        val lead = sorted[0].first
        val second = sorted[1].second
        val leadScore = sorted[0].second

        var leadOrd = prev.leadingBranchOrdinal
        var commit = prev.commitmentLevel
        if (stage.ordinal >= tuning.softCommitMinStageOrdinal) {
            val margin = leadScore - second
            val currentLead = LineageBranch.entries.getOrNull(leadOrd) ?: LineageBranch.BALANCED
            val resist = if (commit >= 1) tuning.commitmentResistance else 0f
            if (lead != currentLead && margin > tuning.leadSwitchMargin + resist) {
                leadOrd = lead.ordinal
            }
            commit = when {
                stage.ordinal >= SeedPodGrowthStage.SPECIALIZATION_ESTABLISHED.ordinal -> 3
                stage.ordinal >= SeedPodGrowthStage.SPECIALIZATION_EMERGING.ordinal -> 2
                stage.ordinal >= SeedPodGrowthStage.FIRST_BRANCH_FORMING.ordinal -> 1
                else -> 0
            }
        }

        val readiness = BranchReadiness.compute(stage, maturityHigh, tuning).readiness.coerceIn(0f, 1f)

        return MorphologyBranchState(
            affinities = next,
            branchReadiness = readiness,
            leadingBranchOrdinal = leadOrd,
            commitmentLevel = commit,
        )
    }
}
