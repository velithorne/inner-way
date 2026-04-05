package com.velithorne.innerway.perception

import com.velithorne.innerway.mind.InternalState

/**
 * Stateless snapshot interpreter for secondary consumers (e.g. background logging).
 * The authoritative mind is [com.velithorne.innerway.mind.InternalStateEngine] in the UI process.
 */
class StateInterpreter {

    fun interpret(ctx: EnvironmentalContext): InternalState {
        val energy = ctx.energyRatio.coerceIn(0f, 1f)
        val thermal = ctx.thermalRatio.coerceIn(0f, 1f)

        if (energy < 0.28f) return InternalState.STRESSED
        if (ctx.charging && energy > 0.28f && thermal < 0.4f) return InternalState.RECOVERING

        val hungry = energy < 0.18f && !ctx.charging
        val stressed = thermal > 0.82f || ctx.nervousLoad > 0.85f
        val resting = ctx.circadianPhase < 0.25f && ctx.motionEnergy < 0.08f && energy > 0.35f

        return when {
            stressed -> InternalState.STRESSED
            hungry -> InternalState.HUNGRY
            resting -> InternalState.RESTING
            else -> InternalState.CALM
        }
    }
}
