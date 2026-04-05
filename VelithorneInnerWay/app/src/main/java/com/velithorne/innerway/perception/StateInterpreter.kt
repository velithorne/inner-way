package com.velithorne.innerway.perception

import com.velithorne.innerway.mind.InternalState

/**
 * Maps fused environment to discrete internal states using weighted rules (Phase 4 expands).
 */
class StateInterpreter {

    fun interpret(ctx: EnvironmentalContext): InternalState {
        val hungry = ctx.energyRatio < 0.18f && !ctx.charging
        val stressed = ctx.thermalRatio > 0.82f || ctx.nervousLoad > 0.85f
        val resting = ctx.circadianPhase < 0.25f && ctx.motionEnergy < 0.08f && ctx.energyRatio > 0.35f

        return when {
            stressed -> InternalState.STRESSED
            hungry -> InternalState.HUNGRY
            resting -> InternalState.RESTING
            else -> InternalState.CALM
        }
    }
}
