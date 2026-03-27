package com.velithorne.vessel.progression

import com.velithorne.vessel.growth_seedpod.SeedPodDisplayState
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage
import com.velithorne.vessel.physiology.PhysiologySnapshot
import kotlin.math.max
import kotlin.math.min

/**
 * Monotonic structural progression: milestones, dwell, accumulation toward post-mature stages.
 */
object DevelopmentEngine {

    data class StepResult(
        val structural: StructuralGrowthState,
        val milestonesUnlocked: List<DevelopmentMilestone>,
    )

    fun step(
        prev: StructuralGrowthState,
        liveDisplay: SeedPodDisplayState,
        maturityScore: Float,
        phys: PhysiologySnapshot,
        dtSec: Float,
        nowMs: Long,
        tuning: ProgressionTuning,
    ): StepResult {
        val s = phys.species
        val strain = (s.stress * 0.5f + s.fever * 0.35f + (1f - s.vitality) * 0.15f).coerceIn(0f, 1f)
        val slowdown = if (strain < tuning.strainSlowdownStart) {
            1f
        } else {
            val t = ((strain - tuning.strainSlowdownStart) / (1f - tuning.strainSlowdownStart + 1e-4f)).coerceIn(0f, 1f)
            1f - t * tuning.strainSlowdownMax
        }

        var flags = prev.milestoneFlags
        val newMilestones = mutableListOf<DevelopmentMilestone>()

        fun unlock(m: DevelopmentMilestone) {
            if (!MilestoneBits.has(flags, m)) {
                flags = MilestoneBits.with(flags, m)
                newMilestones.add(m)
            }
        }

        if (liveDisplay.crownNub >= 0.2f) unlock(DevelopmentMilestone.CROWN_BUD)
        if (max(liveDisplay.lateralBudLeft, liveDisplay.lateralBudRight) >= 0.18f) {
            unlock(DevelopmentMilestone.LATERAL_BUDS)
        }
        if (liveDisplay.reserveBulb >= 0.2f) unlock(DevelopmentMilestone.RESERVE_BULB)
        if (liveDisplay.shellThickening >= 0.28f) unlock(DevelopmentMilestone.SHELL_BAND)
        if (liveDisplay.tissueHaze >= 0.35f) unlock(DevelopmentMilestone.CHAMBER_ENVELOPE)

        val highMaturity = max(prev.confirmedMaturityHigh, maturityScore)
        if (highMaturity >= tuning.chamberMaturedMaturity) {
            unlock(DevelopmentMilestone.CHAMBER_MATURED)
        }

        val lerp = (tuning.tendencyLerp * dtSec * 60f / 16f).coerceIn(0.001f, 0.12f)
        fun ema(prevV: Float, target: Float) = prevV + (target - prevV) * lerp
        var lt = ema(prev.leanThermal, s.fever)
        var ln = ema(prev.leanNeural, s.neuralActivity)
        var ls = ema(prev.leanSignal, s.signalArousal)
        var lr = ema(prev.leanReserve, s.hunger)
        val tsum = lt + ln + ls + lr
        if (tsum > 1e-4f) {
            lt /= tsum; ln /= tsum; ls /= tsum; lr /= tsum
        }

        var stage = prev.permanentStage
        var accum = prev.nextStageAccum
        var entered = prev.stageEnteredAtMs

        fun dwellMet(st: SeedPodGrowthStage): Boolean {
            val idx = st.ordinal.coerceIn(0, tuning.minDwellMsByStageOrdinal.lastIndex)
            val need = tuning.minDwellMsByStageOrdinal.getOrElse(idx) { 60_000L }
            return nowMs - entered >= need
        }

        fun tryAdvanceOnce(): Boolean {
            val hy = tuning.stageAdvanceHysteresis
            val nextOrd = stage.ordinal + 1
            val next = SeedPodGrowthStage.entries.getOrNull(nextOrd) ?: return false
            if (!dwellMet(stage)) return false

            val ok = when (stage) {
                SeedPodGrowthStage.DORMANT_POD -> highMaturity >= 0.14f + hy
                SeedPodGrowthStage.ACTIVATING_POD -> highMaturity >= 0.28f + hy
                SeedPodGrowthStage.GERMINATING_POD -> highMaturity >= 0.42f + hy
                SeedPodGrowthStage.EARLY_BUDDING -> highMaturity >= 0.58f + hy
                SeedPodGrowthStage.EARLY_CHAMBERING -> highMaturity >= tuning.chamberMaturedMaturity + hy
                SeedPodGrowthStage.CHAMBER_MATURED -> accum >= tuning.lineageDiffAccum
                SeedPodGrowthStage.LINEAGE_DIFFERENTIATING -> accum >= tuning.firstBranchAccum
                SeedPodGrowthStage.FIRST_BRANCH_FORMING -> accum >= tuning.branchStabilizingAccum
                SeedPodGrowthStage.BRANCH_STABILIZING -> accum >= tuning.specializationEmergingAccum
                SeedPodGrowthStage.SPECIALIZATION_EMERGING -> accum >= tuning.specializationEstablishedAccum
                SeedPodGrowthStage.SPECIALIZATION_ESTABLISHED -> false
            }
            if (!ok) return false
            stage = next
            accum = 0f
            entered = nowMs
            when (next) {
                SeedPodGrowthStage.LINEAGE_DIFFERENTIATING -> unlock(DevelopmentMilestone.LINEAGE_DIFFERENTIATION)
                SeedPodGrowthStage.FIRST_BRANCH_FORMING -> unlock(DevelopmentMilestone.FIRST_BRANCH_FORM)
                SeedPodGrowthStage.BRANCH_STABILIZING -> unlock(DevelopmentMilestone.ADAPTIVE_SHELL)
                SeedPodGrowthStage.SPECIALIZATION_ESTABLISHED -> unlock(DevelopmentMilestone.SPECIALIZATION_READY)
                else -> {}
            }
            return true
        }

        if (stage.ordinal >= SeedPodGrowthStage.CHAMBER_MATURED.ordinal &&
            stage.ordinal < SeedPodGrowthStage.SPECIALIZATION_ESTABLISHED.ordinal
        ) {
            val bias = (ln * 0.04f + ls * 0.04f + lt * 0.03f + (1f - lr) * 0.02f)
            val rate = tuning.baseAccumPerSec * slowdown * (1f + bias)
            accum = min(1f, accum + rate * dtSec)
        }

        var guard = 0
        while (guard < 16 && tryAdvanceOnce()) {
            guard++
        }

        val floorOrdinal = prev.permanentStage.ordinal
        if (stage.ordinal < floorOrdinal) {
            stage = SeedPodGrowthStage.entries[floorOrdinal]
        }

        val lastIdx = (SeedPodGrowthStage.entries.size - 1).coerceAtLeast(1)
        val stageSlot = stage.ordinal / lastIdx.toFloat()
        val targetProgress = (stageSlot * 0.55f + min(1f, accum) * 0.28f + highMaturity * 0.17f).coerceIn(0f, 1f)
        val alpha = tuning.progressBarSmoothing.coerceIn(0.04f, 0.22f)
        var smoothed = prev.smoothedStructuralProgress + (targetProgress - prev.smoothedStructuralProgress) * alpha
        smoothed = max(prev.smoothedStructuralProgress, smoothed).coerceIn(0f, 1f)

        val out = StructuralGrowthState(
            permanentStage = stage,
            milestoneFlags = flags,
            stageEnteredAtMs = entered,
            nextStageAccum = accum.coerceIn(0f, 1f),
            confirmedMaturityHigh = highMaturity,
            leanThermal = lt,
            leanNeural = ln,
            leanSignal = ls,
            leanReserve = lr,
            smoothedStructuralProgress = smoothed,
            morphologyBranch = prev.morphologyBranch,
        )
        return StepResult(out, newMilestones)
    }
}
