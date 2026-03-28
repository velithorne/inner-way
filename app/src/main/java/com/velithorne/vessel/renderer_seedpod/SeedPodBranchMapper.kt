package com.velithorne.vessel.renderer_seedpod

import kotlin.math.abs
import com.velithorne.vessel.branching.BranchVisualRule
import com.velithorne.vessel.branching.BranchingTuning
import com.velithorne.vessel.branching.LineageBranch
import com.velithorne.vessel.branching.MorphologyBranchState
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage
import com.velithorne.vessel.model.BranchVisualState

/**
 * Maps persisted branching + structural stage into visual scalars for the seed pod.
 * Combines affinity-weighted [BranchVisualRules] with lead-based asymmetry.
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
        val visGate = tuning.visualInfluenceAt(stage) * r * (0.45f + commit * 0.18f)
        val ruleStrength = (visGate * tuning.visualDifferentiationStrengthMul).coerceIn(0f, 1.15f)
        val rules = BranchVisualRule.blend(branch.affinities, ruleStrength)

        fun mulFor(b: LineageBranch, base: Float, delta: Float): Float {
            val isLead = b == lead
            val aff = branch.affinities[b].coerceIn(0f, 1f)
            val t = if (isLead) visGate else visGate * 0.35f * aff
            return (base + delta * t).coerceIn(0.82f, 1.38f)
        }

        val a = branch.affinities
        val shell = mulFor(LineageBranch.THERMAL_SHELL, rules.shellBandMul, 0.06f)
        val lateral = mulFor(LineageBranch.SIGNAL_FROND, rules.lateralFrondMul, 0.08f)
        val crown = mulFor(LineageBranch.CROWN_NEURAL, rules.crownBloomMul, 0.07f)
        val reserve = mulFor(LineageBranch.RESERVE_BASIN, rules.reserveBulbMul, 0.06f)
        val inner = mulFor(LineageBranch.ARCHIVE_CORE, rules.innerMassMul, 0.06f)
        val brace = mulFor(LineageBranch.MOTION_BRACED, rules.bracingMul, 0.05f)
        val balanced = (1f - visGate * 0.22f).coerceIn(0.72f, 1f)

        val stretchX = (rules.contourStretchXMul * (1f + visGate * (0.04f * a.signalFrond - 0.03f * a.motionBraced))).coerceIn(0.9f, 1.18f)
        val stretchY = (rules.contourStretchYMul * (1f + visGate * (0.04f * a.reserveBasin + 0.04f * a.crownNeural - 0.04f * a.thermalShell))).coerceIn(0.9f, 1.18f)
        val reach = (rules.lateralReachMul * (1f + visGate * 0.08f * a.signalFrond)).coerceIn(0.9f, 1.28f)
        val asymBoost = (visGate * 0.35f * abs(a.signalFrond - a.balanced)).coerceIn(0f, 0.14f)
        val bracingAlpha = (visGate * (0.55f * a.motionBraced + 0.15f * a.thermalShell)).coerceIn(0f, 0.5f)

        val exprMag = (visGate * r).coerceIn(0f, 1f)

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
            innerVolumeFocusY = rules.innerVolumeFocusY.coerceIn(-0.55f, 0.55f),
            shellThicknessMul = rules.shellThicknessMul.coerceIn(0.88f, 1.2f),
            thermalVeilEmphasisMul = rules.thermalVeilEmphasisMul.coerceIn(0.85f, 1.25f),
            paletteWarmthBias = rules.paletteWarmthBias,
            paletteCoolSideBias = rules.paletteCoolSideBias,
            paletteCrownTintBias = rules.paletteCrownTintBias,
            paletteReserveTintBias = rules.paletteReserveTintBias,
            visualExpressionMagnitude = exprMag,
        )
    }
}
