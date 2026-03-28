package com.velithorne.vessel.growthtime

import com.velithorne.vessel.morphogenesis.GerminationStage

object StageGateEngine {

    fun mapGerminationToTemporal(g: GerminationStage): TemporalGrowthStage = when (g) {
        GerminationStage.DORMANT_SEED -> TemporalGrowthStage.DORMANT_SEED
        GerminationStage.ACTIVATED_SEED -> TemporalGrowthStage.ACTIVATED_SEED
        GerminationStage.GERMINATING -> TemporalGrowthStage.GERMINATING
        GerminationStage.CHAMBER_FORMATION -> TemporalGrowthStage.CHAMBER_DEEPENING
        GerminationStage.BRANCHING -> TemporalGrowthStage.LATERAL_BUDDING
        GerminationStage.RESERVOIR_DEEPENING -> TemporalGrowthStage.CHAMBER_DEEPENING
        GerminationStage.SHELL_THICKENING -> TemporalGrowthStage.SHELL_ACCRETING
        GerminationStage.STABILIZING -> TemporalGrowthStage.STABILIZING
    }

    /**
     * Move at most one step toward environment-suggested stage after minimum dwell.
     */
    fun stepTowardTarget(
        current: TemporalGrowthStage,
        targetGermination: GerminationStage,
        nowMs: Long,
        stageEnteredAtMs: Long,
        tuning: TimeTuning,
    ): Pair<TemporalGrowthStage, Long> {
        val mapped = mapGerminationToTemporal(targetGermination)
        val order = TemporalGrowthStage.entries
        val ci = order.indexOf(current).coerceIn(0, order.lastIndex)
        val ti = order.indexOf(mapped).coerceIn(0, order.lastIndex)
        val minDwell = dwellFor(current, tuning)
        if (nowMs - stageEnteredAtMs < minDwell) return current to stageEnteredAtMs
        return when {
            ti > ci && ci < order.lastIndex -> order[ci + 1] to nowMs
            ti < ci && ci > 0 -> order[ci - 1] to nowMs
            else -> current to stageEnteredAtMs
        }
    }

    private fun dwellFor(s: TemporalGrowthStage, t: TimeTuning): Long = when (s) {
        TemporalGrowthStage.DORMANT_SEED -> t.dwellDormantMs
        TemporalGrowthStage.ACTIVATED_SEED -> t.dwellActivatedMs
        TemporalGrowthStage.GERMINATING -> t.dwellGerminatingMs
        TemporalGrowthStage.CROWN_FORMING -> t.dwellCrownMs
        TemporalGrowthStage.LATERAL_BUDDING -> t.dwellLateralMs
        TemporalGrowthStage.CHAMBER_DEEPENING -> t.dwellChamberMs
        TemporalGrowthStage.SHELL_ACCRETING -> t.dwellShellMs
        TemporalGrowthStage.STABILIZING -> 60_000L
    }
}
