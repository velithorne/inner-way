package com.velithorne.vessel.data.db.mapper

import com.velithorne.vessel.data.db.entity.SeedPodStateEntity
import com.velithorne.vessel.growth_seedpod.SeedPodDisplayState
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthBudget
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthState

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
        return SeedPodGrowthState(display = d, budget = b)
    }

    fun toEntity(
        specimenId: String,
        state: SeedPodGrowthState,
        lastVisibleGrowthMs: Long,
        lastStageTransitionMs: Long,
        lastAdaptationUpdateMs: Long,
    ): SeedPodStateEntity =
        SeedPodStateEntity(
            specimenId = specimenId,
            stageOrdinal = state.display.stage.ordinal,
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
        )
}
