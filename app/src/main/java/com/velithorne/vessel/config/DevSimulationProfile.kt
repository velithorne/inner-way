package com.velithorne.vessel.config

import com.velithorne.vessel.background.BackgroundTuning
import com.velithorne.vessel.branching.BranchingTuning
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthTuning
import com.velithorne.vessel.growthtime.TimeTuning
import com.velithorne.vessel.progression.ProgressionTuning
import java.util.concurrent.TimeUnit

/**
 * Accelerated pacing for debug/development — fresh specimen per build via [TimeTuning.buildResetEnabled].
 */
object DevSimulationProfile {

    fun build(): GrowthProfile {
        val prog = ProgressionTuning(
            minDwellMsByStageOrdinal = longArrayOf(
                0L,
                4_000L,
                5_000L,
                7_000L,
                9_000L,
                12_000L,
                18_000L,
                22_000L,
                26_000L,
                30_000L,
                34_000L,
                38_000L,
            ),
            baseAccumPerSec = 0.00065f,
            strainSlowdownMax = 0.35f,
            strainSlowdownStart = 0.45f,
            stageAdvanceHysteresis = 0.025f,
            chamberMaturedMaturity = 0.62f,
            lineageDiffAccum = 0.72f,
            firstBranchAccum = 0.76f,
            branchStabilizingAccum = 0.8f,
            specializationEmergingAccum = 0.84f,
            specializationEstablishedAccum = 0.88f,
            tendencyLerp = 0.045f,
            progressBarSmoothing = 0.14f,
        )
        val branch = BranchingTuning(
            affinityLerpPerSec = 0.032f,
            affinityDeltaCap = 0.07f,
            leadSwitchMargin = 0.04f,
            commitmentResistance = 0.08f,
            branchReadinessEventThreshold = 0.22f,
            branchAffinityStrengthenDelta = 0.022f,
        )
        val bg = BackgroundTuning(
            periodicWorkIntervalMs = TimeUnit.MINUTES.toMillis(15),
            devPeriodicWorkIntervalMs = TimeUnit.MINUTES.toMillis(5),
            snapshotWriteDebounceMs = 15_000L,
            maxAmbientCatchUpSimulatedSec = 180f,
            ambientCatchUpStepSec = 2f,
            minAwayMsForAmbientProcess = 1_500L,
            chargingSampleStrongThreshold = 2,
            cellularSampleStrongThreshold = 2,
            ambientStructuralReadinessScale = 0.09f,
            maxAmbientStructuralReadinessPerApply = 0.065f,
            ambientAffinityNudgeScale = 0.055f,
            maxAmbientAffinityNudgePerApply = 0.04f,
            returnSummaryReadinessMentionThreshold = 0.004f,
            returnSummaryAffinityMentionThreshold = 0.003f,
        )
        val time = TimeTuning(
            offlineCatchUpStepMs = 30_000L,
            maxOfflineCatchUpMs = 45 * 60 * 1000L,
            maxOfflineCatchUpVisualJump = 0.28f,
            buildResetEnabled = true,
            seedFormBlendSpeed = 0.14f,
            contourMulSpeed = 0.11f,
            seedCoreSpeed = 0.2f,
            chamberMassSpeed = 0.09f,
            growthVisualCueSpeed = 0.12f,
            buddingSpeed = 0.11f,
            budgetGainCrown = 0.022f,
            budgetGainFrond = 0.024f,
            budgetGainReservoir = 0.02f,
            budgetGainShell = 0.016f,
            dwellDormantMs = 4_000L,
            dwellActivatedMs = 5_000L,
            dwellGerminatingMs = 6_000L,
            dwellCrownMs = 8_000L,
            dwellLateralMs = 8_000L,
            dwellChamberMs = 10_000L,
            dwellShellMs = 10_000L,
        )
        val seed = SeedPodGrowthTuning(
            budgetGainMultiplier = 2.2f,
            budgetDecayMultiplier = 1.15f,
            displayLerpMultiplier = 1.65f,
            maturityScoreMultiplier = 1.12f,
        )
        return GrowthProfile(
            mode = SimulationMode.DEV_SIMULATION,
            progression = prog,
            branching = branch,
            background = bg,
            time = time,
            seedPodGrowth = seed,
            seedPodMaxOfflineCatchUpMs = 300_000L,
        )
    }
}
