package com.velithorne.vessel.growthtime

import com.velithorne.vessel.morphogenesis.MorphogenesisSnapshot

object OfflineCatchUpEngine {

    /**
     * Coarse multi-step catch-up for elapsed time while app was closed.
     * Returns number of steps applied and remaining budget for reveal.
     */
    fun simulateCatchUp(
        elapsedMs: Long,
        tuning: TimeTuning,
        onStep: (dtSec: Float, target: MorphogenesisSnapshot) -> Unit,
        lastTarget: MorphogenesisSnapshot,
    ): Long {
        var remaining = elapsedMs.coerceAtMost(tuning.maxOfflineCatchUpMs)
        var applied = 0L
        while (remaining > 0L) {
            val step = minOf(remaining, tuning.offlineCatchUpStepMs)
            val dtSec = step / 1000f
            onStep(dtSec, lastTarget)
            applied += step
            remaining -= step
        }
        return applied
    }
}
