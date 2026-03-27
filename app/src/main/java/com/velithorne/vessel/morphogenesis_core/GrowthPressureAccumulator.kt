package com.velithorne.vessel.morphogenesis_core

import com.velithorne.vessel.physiology.PhysiologySnapshot

/**
 * Owns slow EMA of [GrowthPressureState].
 */
class GrowthPressureAccumulator(
    private var state: GrowthPressureState = GrowthPressureState(),
    private val alphaSlow: Float,
    private val alphaFast: Float,
) {
    fun step(phys: PhysiologySnapshot): GrowthPressureState {
        val inst = GrowthPressureEngine.instant(phys, phys.telemetry)
        state = GrowthPressureEngine.step(state, inst, alphaSlow, alphaFast)
        return state
    }

    fun snapshot(): GrowthPressureState = state

    fun restore(s: GrowthPressureState) {
        state = s
    }
}
