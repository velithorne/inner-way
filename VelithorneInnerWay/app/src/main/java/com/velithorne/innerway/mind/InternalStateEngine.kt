package com.velithorne.innerway.mind

import com.velithorne.innerway.perception.EnvironmentalContext
import kotlin.math.max
import kotlin.math.min

/**
 * Resolves internal state from fused perception using weighted evidence, not a single hard rule.
 * [SomaticHints] carries continuity (stillness, disturbance, motion alerts) from the view-model loop.
 */
class InternalStateEngine {

    fun resolve(context: EnvironmentalContext, hints: SomaticHints = SomaticHints()): InternalState {
        val thermal = context.thermalRatio.coerceIn(0f, 1f)
        val nervous = context.nervousLoad.coerceIn(0f, 1f)
        val energy = context.energyRatio.coerceIn(0f, 1f)
        val motion = context.motionEnergy.coerceIn(0f, 1f)

        val stressWeight = 0.55f * thermal + 0.45f * nervous
        val hungryWeight = if (!context.charging && energy < 0.22f) {
            (1f - energy / 0.22f).coerceIn(0f, 1f)
        } else {
            0f
        }
        val stillFactor = (hints.stillnessDurationSeconds / 90f).coerceIn(0f, 1f)
        val motionCalm = (1f - (motion / 0.18f).coerceIn(0f, 1f))
        val nightRest = if (context.isNightWindow) 1f else 0.35f
        val restingWeight = stillFactor * motionCalm * nightRest * if (energy > 0.28f) 1f else 0.35f

        val defensiveWeight = hints.disturbanceScore * 0.85f + stressWeight * 0.25f
        val curiousWeight = (hints.motionAlertSecondsRemaining / 3f).coerceIn(0f, 1f) * 0.95f +
            motion * 0.4f

        val calmWeight = max(0f, 0.55f - stressWeight - hungryWeight * 0.35f + (1f - motion) * 0.08f)

        data class Weighted(val state: InternalState, val w: Float)

        val candidates = listOf(
            Weighted(InternalState.STRESSED, stressWeight),
            Weighted(InternalState.HUNGRY, hungryWeight * 0.95f),
            Weighted(InternalState.RESTING, restingWeight),
            Weighted(InternalState.DEFENSIVE, defensiveWeight),
            Weighted(InternalState.CURIOUS, curiousWeight),
            Weighted(InternalState.CALM, calmWeight),
        )

        val best = candidates.maxBy { it.w }

        // Disturbance can override stress into a protective shell.
        if (defensiveWeight > 0.62f && defensiveWeight >= stressWeight * 0.82f) {
            return InternalState.DEFENSIVE
        }
        if (stressWeight > 0.72f && hungryWeight < 0.7f) {
            return InternalState.STRESSED
        }
        if (hungryWeight > 0.68f) {
            return InternalState.HUNGRY
        }
        if (restingWeight > 0.58f && stressWeight < 0.55f && hungryWeight < 0.55f) {
            return InternalState.RESTING
        }
        if (curiousWeight > 0.48f && stressWeight < 0.58f && hungryWeight < 0.55f) {
            return InternalState.CURIOUS
        }

        return if (best.w > 0.18f) best.state else InternalState.CALM
    }
}
