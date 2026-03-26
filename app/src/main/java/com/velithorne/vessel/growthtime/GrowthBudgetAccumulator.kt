package com.velithorne.vessel.growthtime

import com.velithorne.vessel.morphogenesis.PressureAccumulator
import com.velithorne.vessel.util.Smoothing

object GrowthBudgetAccumulator {

    fun accumulate(
        prev: GrowthBudget,
        acc: PressureAccumulator,
        dtSec: Float,
        tuning: TimeTuning,
    ): GrowthBudget {
        val d = dtSec.coerceIn(0f, 120f)
        fun add(base: Float, pressure: Float, gain: Float) =
            (base + pressure * gain * d - tuning.budgetDecayPerSecond * base * d * 0.3f).coerceIn(0f, 1f)

        return GrowthBudget(
            crownGrowthBudget = add(prev.crownGrowthBudget, acc.neural, tuning.budgetGainCrown),
            frondGrowthBudget = add(prev.frondGrowthBudget, acc.signal, tuning.budgetGainFrond),
            reservoirGrowthBudget = add(prev.reservoirGrowthBudget, acc.archive * 0.5f + acc.hunger * 0.35f, tuning.budgetGainReservoir),
            shellGrowthBudget = add(prev.shellGrowthBudget, acc.thermal, tuning.budgetGainShell),
            archiveGrowthBudget = add(prev.archiveGrowthBudget, acc.archive, tuning.budgetGainArchive),
            tendonGrowthBudget = add(prev.tendonGrowthBudget, acc.motion, tuning.budgetGainTendon),
            recoveryRepairBudget = add(prev.recoveryRepairBudget, acc.recovery, tuning.budgetGainRecovery),
        )
    }
}
