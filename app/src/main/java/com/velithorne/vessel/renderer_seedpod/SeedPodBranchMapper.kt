package com.velithorne.vessel.renderer_seedpod

import kotlin.math.abs
import com.velithorne.vessel.branching.BranchingTuning
import com.velithorne.vessel.branching.LineageBranch
import com.velithorne.vessel.branching.MorphologyBranchState
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage
import com.velithorne.vessel.model.BranchVisualState

/**
 * Maps persisted branching + structural stage into subtle visual scalars for the seed pod.
 */
object SeedPodBranchMapper {

    fun map(
        branch: MorphologyBranchState,
        stage: SeedPodGrowthStage,
        tuning: BranchingTuning = BranchingTuning(),
    ): BranchVisualState {
        if (stage.ordinal < tuning.branchReadinessUnlockStageOrdinal) {
            return BranchVisualState.neutral()
        }
        val lead = branch.leadingBranch()
        val r = branch.branchReadiness.coerceIn(0f, 1f)
        val commit = branch.commitmentLevel.coerceIn(0, 3)
        val vis = tuning.visualInfluenceAt(stage) * r * (0.45f + commit * 0.18f)

        fun mulFor(b: LineageBranch, base: Float, delta: Float): Float {
            val isLead = b == lead
            val aff = branch.affinities[b].coerceIn(0f, 1f)
            val t = if (isLead) vis else vis * 0.35f * aff
            return (base + delta * t).coerceIn(0.85f, 1.35f)
        }

        val a = branch.affinities
        val shell = mulFor(LineageBranch.THERMAL_SHELL, 1f, 0.12f)
        val lateral = mulFor(LineageBranch.SIGNAL_FROND, 1f, 0.14f)
        val crown = mulFor(LineageBranch.CROWN_NEURAL, 1f, 0.11f)
        val reserve = mulFor(LineageBranch.RESERVE_BASIN, 1f, 0.1f)
        val inner = mulFor(LineageBranch.ARCHIVE_CORE, 1f, 0.09f)
        val brace = mulFor(LineageBranch.MOTION_BRACED, 1f, 0.08f)
        val balanced = (1f - vis * 0.25f).coerceIn(0.75f, 1f)

        val stretchX = (1f + vis * (0.06f * a.thermalShell + 0.04f * a.signalFrond - 0.05f * a.motionBraced)).coerceIn(0.92f, 1.12f)
        val stretchY = (1f + vis * (0.05f * a.reserveBasin + 0.04f * a.crownNeural - 0.04f * a.thermalShell)).coerceIn(0.92f, 1.12f)
        val reach = (1f + vis * 0.1f * a.signalFrond).coerceIn(0.95f, 1.22f)
        val asymBoost = (vis * 0.35f * abs(a.signalFrond - a.balanced)).coerceIn(0f, 0.12f)
        val bracingAlpha = (vis * (0.55f * a.motionBraced + 0.15f * a.thermalShell)).coerceIn(0f, 0.45f)

        return BranchVisualState(
            leadingBranch = lead,
            branchReadiness = r,
            commitmentLevel = commit,
            shellBandMul = shell,
            lateralFrondMul = lateral,
            crownBloomMul = crown,
            reserveBulbMul = reserve,
            innerMassMul = inner,
            bracingMul = brace,
            balancedBlend = balanced,
            contourStretchXMul = stretchX,
            contourStretchYMul = stretchY,
            lateralReachMul = reach,
            lateralAsymmetryBoost = asymBoost,
            bracingLineAlpha = bracingAlpha,
        )
    }
}
