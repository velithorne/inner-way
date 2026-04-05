package com.velithorne.innerway.mind

import com.velithorne.innerway.perception.EnvironmentalContext
import kotlin.math.max

/**
 * Resolves internal state from fused perception + [SomaticHints], with hysteresis for leaving
 * STRESSED and explicit battery / RECOVERING / CALM transitions.
 */
class InternalStateEngine {

    private var lastState: InternalState = InternalState.CALM
    /** Time spent with improved conditions while last state was STRESSED — must reach [STRESSED_EXIT_SECONDS]. */
    private var stressedExitProgressSeconds: Float = 0f

    fun resolve(
        context: EnvironmentalContext,
        hints: SomaticHints = SomaticHints(),
        deltaTimeSeconds: Float,
    ): InternalState {
        val dt = deltaTimeSeconds.coerceIn(0f, 5f)
        val thermal = context.thermalRatio.coerceIn(0f, 1f)
        val nervous = context.nervousLoad.coerceIn(0f, 1f)
        val energy = context.energyRatio.coerceIn(0f, 1f)
        val motion = context.motionEnergy.coerceIn(0f, 1f)
        val stressWeight = 0.55f * thermal + 0.45f * nervous

        val batteryForcedStressed = energy < BATTERY_STRESS_THRESHOLD
        val recoveringConditions =
            context.charging && energy > BATTERY_STRESS_THRESHOLD && thermal < RECOVERING_HEAT_MAX
        val calmStableConditions =
            energy > CALM_ENERGY_MIN &&
                hints.stableRecoverySeconds >= CALM_STABLE_SECONDS &&
                thermal < 0.55f &&
                nervous < 0.75f

        // --- Law: low battery → STRESSED
        if (batteryForcedStressed) {
            stressedExitProgressSeconds = 0f
            lastState = InternalState.STRESSED
            return InternalState.STRESSED
        }

        // --- Hysteresis: do not leave STRESSED until improved conditions hold for a time window
        if (lastState == InternalState.STRESSED) {
            val improvedEnough = energy >= BATTERY_STRESS_THRESHOLD && stressWeight <= STRESSED_RELIEF_MAX
            if (!improvedEnough) {
                stressedExitProgressSeconds = 0f
                return InternalState.STRESSED
            }
            stressedExitProgressSeconds += dt
            if (stressedExitProgressSeconds < STRESSED_EXIT_SECONDS) {
                return InternalState.STRESSED
            }
        } else {
            stressedExitProgressSeconds = 0f
        }

        // --- Recovery: feeding while cool (after stress exit window when previously stressed)
        if (recoveringConditions) {
            lastState = InternalState.RECOVERING
            return InternalState.RECOVERING
        }

        // --- Calm: sustained stable window + adequate charge + low motion
        if (calmStableConditions && motion < MOTION_CALM_MAX) {
            lastState = InternalState.CALM
            return InternalState.CALM
        }

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
        val motionSpike = hints.motionAlertSecondsRemaining > 0.05f || motion > MOTION_SPIKE_THRESHOLD
        val curiousWeight =
            (hints.motionAlertSecondsRemaining / 3f).coerceIn(0f, 1f) * 0.95f + motion * 0.45f
        val alertWeight =
            if (motionSpike && motion > ALERT_MOTION_THRESHOLD) 0.55f + motion * 0.35f else 0f

        val calmWeight = max(0f, 0.55f - stressWeight - hungryWeight * 0.35f + (1f - motion) * 0.08f)

        data class Weighted(val state: InternalState, val w: Float)

        val candidates = listOf(
            Weighted(InternalState.STRESSED, stressWeight),
            Weighted(InternalState.HUNGRY, hungryWeight * 0.95f),
            Weighted(InternalState.RESTING, restingWeight),
            Weighted(InternalState.DEFENSIVE, defensiveWeight),
            Weighted(InternalState.CURIOUS, curiousWeight),
            Weighted(InternalState.ALERT, alertWeight),
            Weighted(InternalState.CALM, calmWeight),
        )

        val best = candidates.maxBy { it.w }

        if (defensiveWeight > 0.62f && defensiveWeight >= stressWeight * 0.82f) {
            lastState = InternalState.DEFENSIVE
            return InternalState.DEFENSIVE
        }
        if (stressWeight > 0.72f && hungryWeight < 0.7f && !recoveringConditions) {
            lastState = InternalState.STRESSED
            return InternalState.STRESSED
        }
        if (hungryWeight > 0.68f) {
            lastState = InternalState.HUNGRY
            return InternalState.HUNGRY
        }
        if (restingWeight > 0.58f && stressWeight < 0.55f && hungryWeight < 0.55f && hints.stillnessDurationSeconds > 25f) {
            lastState = InternalState.RESTING
            return InternalState.RESTING
        }
        if (alertWeight > 0.52f && stressWeight < 0.6f) {
            lastState = InternalState.ALERT
            return InternalState.ALERT
        }
        if (curiousWeight > 0.48f && stressWeight < 0.58f && hungryWeight < 0.55f) {
            lastState = InternalState.CURIOUS
            return InternalState.CURIOUS
        }

        val resolved = if (best.w > 0.18f) best.state else InternalState.CALM
        lastState = resolved
        return resolved
    }

    companion object {
        private const val BATTERY_STRESS_THRESHOLD = 0.28f
        private const val RECOVERING_HEAT_MAX = 0.4f
        private const val CALM_ENERGY_MIN = 0.35f
        private const val CALM_STABLE_SECONDS = 8f
        private const val STRESSED_EXIT_SECONDS = 4f
        /** Must be at or below this stress blend to count as "improved" for leaving STRESSED. */
        private const val STRESSED_RELIEF_MAX = 0.5f
        private const val MOTION_CALM_MAX = 0.12f
        private const val MOTION_SPIKE_THRESHOLD = 0.2f
        private const val ALERT_MOTION_THRESHOLD = 0.35f
    }
}
