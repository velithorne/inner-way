package com.velithorne.vessel.background

import com.velithorne.vessel.growth_seedpod.SeedPodGrowthBudget
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthState
import com.velithorne.vessel.physiology.PhysiologySnapshot
import kotlin.math.min

/**
 * Bounded catch-up from unapplied ecology snapshots — merges budget into state and simulates growth in chunks.
 */
object AmbientReopenProcessor {

    fun mergeBudget(
        state: SeedPodGrowthState,
        delta: SeedPodGrowthBudget,
    ): SeedPodGrowthState {
        val b = state.budget
        fun add(a: Float, d: Float) = (a + d).coerceIn(0f, 1f)
        return state.copy(
            budget = SeedPodGrowthBudget(
                crown = add(b.crown, delta.crown),
                lateral = add(b.lateral, delta.lateral),
                reserve = add(b.reserve, delta.reserve),
                shell = add(b.shell, delta.shell),
                thermal = add(b.thermal, delta.thermal),
                coherence = add(b.coherence, delta.coherence),
            ),
        )
    }

    /**
     * Returns total simulated seconds (may be less than wall away time — bounded).
     */
    fun simulateGrowthSteps(
        initial: SeedPodGrowthState,
        phys: PhysiologySnapshot,
        dtSec: Float,
        advance: (PhysiologySnapshot, SeedPodGrowthState, Float) -> SeedPodGrowthState,
    ): Pair<SeedPodGrowthState, Float> {
        var s = initial
        var remaining = dtSec
        var total = 0f
        val step = maxOf(0.5f, 1f)
        while (remaining > 1e-3f) {
            val chunk = min(step, remaining)
            s = advance(phys, s, chunk)
            total += chunk
            remaining -= chunk
        }
        return s to total
    }
}
