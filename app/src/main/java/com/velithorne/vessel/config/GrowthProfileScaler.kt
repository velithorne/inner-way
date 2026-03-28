package com.velithorne.vessel.config

import com.velithorne.vessel.branching.BranchingTuning
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthTuning
import com.velithorne.vessel.growthtime.TimeTuning
import com.velithorne.vessel.progression.ProgressionTuning
import kotlin.math.roundToLong

/**
 * Scales dev [GrowthProfile] by a user-chosen evolution speed multiplier (1 = default dev pacing).
 */
object GrowthProfileScaler {

    /** Clamp stored / UI values to a safe range. */
    fun clampMultiplier(raw: Float): Float = raw.coerceIn(0.25f, 8f)

    fun scale(profile: GrowthProfile, speed: Float): GrowthProfile {
        val s = clampMultiplier(speed)
        if (s == 1f) return profile
        return profile.copy(
            progression = scaleProgression(profile.progression, s),
            branching = scaleBranching(profile.branching, s),
            background = profile.background,
            time = scaleTime(profile.time, s),
            seedPodGrowth = scaleSeedPodGrowth(profile.seedPodGrowth, s),
            seedPodMaxOfflineCatchUpMs = scaleMs(profile.seedPodMaxOfflineCatchUpMs, s).coerceAtLeast(30_000L),
        )
    }

    private fun scaleProgression(t: ProgressionTuning, s: Float): ProgressionTuning {
        val dwell = LongArray(t.minDwellMsByStageOrdinal.size) { i ->
            scaleMs(t.minDwellMsByStageOrdinal[i], 1f / s)
        }
        return t.copy(
            minDwellMsByStageOrdinal = dwell,
            baseAccumPerSec = t.baseAccumPerSec * s,
            tendencyLerp = (t.tendencyLerp * s).coerceIn(0.001f, 0.5f),
            progressBarSmoothing = (t.progressBarSmoothing * s).coerceIn(0.02f, 0.5f),
        )
    }

    private fun scaleBranching(t: BranchingTuning, s: Float): BranchingTuning =
        t.copy(
            affinityLerpPerSec = t.affinityLerpPerSec * s,
            affinityDeltaCap = (t.affinityDeltaCap * s).coerceAtMost(0.35f),
            branchReadinessEventThreshold = (t.branchReadinessEventThreshold * s).coerceIn(0.05f, 0.5f),
            branchAffinityStrengthenDelta = t.branchAffinityStrengthenDelta * s,
            branchVisualReinforceDelta = t.branchVisualReinforceDelta * s,
        )

    private fun scaleTime(t: TimeTuning, s: Float): TimeTuning =
        t.copy(
            offlineCatchUpStepMs = scaleMs(t.offlineCatchUpStepMs, 1f / s).coerceAtLeast(5_000L),
            maxOfflineCatchUpMs = scaleMs(t.maxOfflineCatchUpMs, 1f / s).coerceAtLeast(60_000L),
            maxOfflineCatchUpVisualJump = (t.maxOfflineCatchUpVisualJump * s).coerceIn(0.05f, 0.95f),
            seedFormBlendSpeed = t.seedFormBlendSpeed * s,
            contourMulSpeed = t.contourMulSpeed * s,
            seedCoreSpeed = t.seedCoreSpeed * s,
            chamberMassSpeed = t.chamberMassSpeed * s,
            growthVisualCueSpeed = t.growthVisualCueSpeed * s,
            buddingSpeed = t.buddingSpeed * s,
            maxCrownMulDeltaPerMinute = t.maxCrownMulDeltaPerMinute * s,
            maxFrondVisualDeltaPerMinute = t.maxFrondVisualDeltaPerMinute * s,
            maxReservoirDeltaPerMinute = t.maxReservoirDeltaPerMinute * s,
            maxShellDeltaPerHour = t.maxShellDeltaPerHour * s,
            budgetGainCrown = t.budgetGainCrown * s,
            budgetGainFrond = t.budgetGainFrond * s,
            budgetGainReservoir = t.budgetGainReservoir * s,
            budgetGainShell = t.budgetGainShell * s,
            budgetGainArchive = t.budgetGainArchive * s,
            budgetGainTendon = t.budgetGainTendon * s,
            budgetGainRecovery = t.budgetGainRecovery * s,
            budgetDecayPerSecond = t.budgetDecayPerSecond * s,
            budgetSpendToLerpBoost = t.budgetSpendToLerpBoost * s,
            dwellDormantMs = scaleMs(t.dwellDormantMs, 1f / s),
            dwellActivatedMs = scaleMs(t.dwellActivatedMs, 1f / s),
            dwellGerminatingMs = scaleMs(t.dwellGerminatingMs, 1f / s),
            dwellCrownMs = scaleMs(t.dwellCrownMs, 1f / s),
            dwellLateralMs = scaleMs(t.dwellLateralMs, 1f / s),
            dwellChamberMs = scaleMs(t.dwellChamberMs, 1f / s),
            dwellShellMs = scaleMs(t.dwellShellMs, 1f / s),
        )

    private fun scaleSeedPodGrowth(t: SeedPodGrowthTuning, s: Float): SeedPodGrowthTuning =
        t.copy(
            budgetGainMultiplier = t.budgetGainMultiplier * s,
            budgetDecayMultiplier = t.budgetDecayMultiplier * s,
            displayLerpMultiplier = t.displayLerpMultiplier * s,
            maturityScoreMultiplier = (t.maturityScoreMultiplier * (0.85f + 0.15f * s)).coerceIn(0.5f, 3f),
        )

    private fun scaleMs(ms: Long, factor: Float): Long =
        (ms.toDouble() * factor.toDouble()).roundToLong().coerceAtLeast(0L)
}
