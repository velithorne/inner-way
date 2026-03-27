package com.velithorne.vessel.data.db.mapper

import com.velithorne.vessel.data.db.entity.SeedPodStateEntity
import com.velithorne.vessel.growth_seedpod.SeedPodDisplayState
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthBudget
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthState
import com.velithorne.vessel.branching.BranchAffinity
import com.velithorne.vessel.branching.MorphologyBranchState
import com.velithorne.vessel.progression.StructuralGrowthState

object GrowthStateMapper {

    fun toGrowthState(e: SeedPodStateEntity): SeedPodGrowthState {
        val st = SeedPodGrowthStage.entries.getOrNull(e.stageOrdinal) ?: SeedPodGrowthStage.DORMANT_POD
        val d = SeedPodDisplayState(
            stage = st,
            crownNub = e.crownNub,
            lateralBudLeft = e.lateralBudLeft,
            lateralBudRight = e.lateralBudRight,
            reserveBulb = e.reserveBulb,
            shellThickening = e.shellThickening,
            thermalVeil = e.thermalVeil,
            tissueHaze = e.tissueHaze,
            podCoherence = e.podCoherence,
            lastWallClockMs = e.lastWallClockMs,
        )
        val b = SeedPodGrowthBudget(
            crown = e.budgetCrown,
            lateral = e.budgetLateral,
            reserve = e.budgetReserve,
            shell = e.budgetShell,
            thermal = e.budgetThermal,
            coherence = e.budgetCoherence,
        )
        val branch = MorphologyBranchState(
            affinities = BranchAffinity.fromArray(
                floatArrayOf(
                    e.affinity0, e.affinity1, e.affinity2, e.affinity3,
                    e.affinity4, e.affinity5, e.affinity6,
                ),
            ),
            branchReadiness = e.branchReadiness,
            leadingBranchOrdinal = e.leadingBranchOrdinal,
            commitmentLevel = e.branchCommitmentLevel,
        )
        val sg = StructuralGrowthState(
            permanentStage = st,
            milestoneFlags = e.milestoneFlags,
            stageEnteredAtMs = e.structuralStageEnteredAtMs.takeIf { it > 0L } ?: e.lastWallClockMs,
            nextStageAccum = e.nextStageAccum,
            confirmedMaturityHigh = e.confirmedMaturityHigh,
            leanThermal = e.leanThermal,
            leanNeural = e.leanNeural,
            leanSignal = e.leanSignal,
            leanReserve = e.leanReserve,
            smoothedStructuralProgress = e.smoothedStructuralProgress,
            morphologyBranch = branch,
        )
        return SeedPodGrowthState(display = d, budget = b, structural = sg)
    }

    fun toEntity(
        specimenId: String,
        state: SeedPodGrowthState,
        lastVisibleGrowthMs: Long,
        lastStageTransitionMs: Long,
        lastAdaptationUpdateMs: Long,
    ): SeedPodStateEntity {
        val aff = state.structural.morphologyBranch.affinities.asArray()
        return SeedPodStateEntity(
            specimenId = specimenId,
            stageOrdinal = state.structural.permanentStage.ordinal,
            crownNub = state.display.crownNub,
            lateralBudLeft = state.display.lateralBudLeft,
            lateralBudRight = state.display.lateralBudRight,
            reserveBulb = state.display.reserveBulb,
            shellThickening = state.display.shellThickening,
            thermalVeil = state.display.thermalVeil,
            tissueHaze = state.display.tissueHaze,
            podCoherence = state.display.podCoherence,
            lastWallClockMs = state.display.lastWallClockMs,
            budgetCrown = state.budget.crown,
            budgetLateral = state.budget.lateral,
            budgetReserve = state.budget.reserve,
            budgetShell = state.budget.shell,
            budgetThermal = state.budget.thermal,
            budgetCoherence = state.budget.coherence,
            lastVisibleGrowthMs = lastVisibleGrowthMs,
            lastStageTransitionMs = lastStageTransitionMs,
            lastAdaptationUpdateMs = lastAdaptationUpdateMs,
            milestoneFlags = state.structural.milestoneFlags,
            nextStageAccum = state.structural.nextStageAccum,
            confirmedMaturityHigh = state.structural.confirmedMaturityHigh,
            leanThermal = state.structural.leanThermal,
            leanNeural = state.structural.leanNeural,
            leanSignal = state.structural.leanSignal,
            leanReserve = state.structural.leanReserve,
            smoothedStructuralProgress = state.structural.smoothedStructuralProgress,
            structuralStageEnteredAtMs = state.structural.stageEnteredAtMs,
            affinity0 = aff[0],
            affinity1 = aff[1],
            affinity2 = aff[2],
            affinity3 = aff[3],
            affinity4 = aff[4],
            affinity5 = aff[5],
            affinity6 = aff[6],
            branchReadiness = state.structural.morphologyBranch.branchReadiness,
            leadingBranchOrdinal = state.structural.morphologyBranch.leadingBranchOrdinal,
            branchCommitmentLevel = state.structural.morphologyBranch.commitmentLevel,
        )
    }
}
