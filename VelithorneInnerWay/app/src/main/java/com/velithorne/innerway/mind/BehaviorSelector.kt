package com.velithorne.innerway.mind

import com.velithorne.innerway.perception.EnvironmentalContext

/**
 * Selects emergent behavior labels from internal state. Phase 4 wires full mapping.
 */
class BehaviorSelector {

    fun select(state: InternalState, context: EnvironmentalContext): String {
        return when (state) {
            InternalState.CALM -> "slow_breath"
            InternalState.HUNGRY -> "inward_dim"
            InternalState.STRESSED -> "protective_contract"
            InternalState.DISTURBED -> "alert_scan"
            InternalState.RECOVERING -> "gentle_pulse"
            InternalState.RESTING -> "dim_dormant"
            InternalState.DORMANT -> "minimal_presence"
            InternalState.ALERT -> "sharp_attention"
            InternalState.CURIOUS -> "micro_scan"
            InternalState.DEFENSIVE -> "narrow_quiet"
        }
    }
}
