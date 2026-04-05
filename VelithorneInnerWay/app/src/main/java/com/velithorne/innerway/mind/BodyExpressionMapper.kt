package com.velithorne.innerway.mind

import com.velithorne.innerway.perception.EnvironmentalContext
import kotlin.math.max
import kotlin.math.min

/**
 * Maps resolved internal state + measured body context into renderable physiology.
 * Device modifiers are applied from real [EnvironmentalContext] and [SomaticHints].
 */
object BodyExpressionMapper {

    fun map(
        state: InternalState,
        environment: EnvironmentalContext,
        hints: SomaticHints,
    ): BodyExpressionModel {
        val base = baseForState(state)
        return applyDeviceModifiers(base, state, environment, hints)
    }

    private fun baseForState(state: InternalState): BodyExpressionModel {
        return when (state) {
            InternalState.CALM -> BodyExpressionModel(
                breathRate = 0.28f,
                breathDepth = 0.72f,
                pulseIntensity = 0.5f,
                brightness = 0.82f,
                contraction = 0.22f,
                instability = 0.08f,
                openness = 0.78f,
                sleepDepth = 0.12f,
            )
            InternalState.HUNGRY -> BodyExpressionModel(
                breathRate = 0.42f,
                breathDepth = 0.38f,
                pulseIntensity = 0.38f,
                brightness = 0.48f,
                contraction = 0.48f,
                instability = 0.1f,
                openness = 0.42f,
                sleepDepth = 0.18f,
            )
            InternalState.STRESSED -> BodyExpressionModel(
                breathRate = 0.78f,
                breathDepth = 0.45f,
                pulseIntensity = 0.82f,
                brightness = 0.58f,
                contraction = 0.72f,
                instability = 0.62f,
                openness = 0.28f,
                sleepDepth = 0.1f,
            )
            InternalState.RESTING -> BodyExpressionModel(
                breathRate = 0.18f,
                breathDepth = 0.88f,
                pulseIntensity = 0.32f,
                brightness = 0.38f,
                contraction = 0.2f,
                instability = 0.04f,
                openness = 0.55f,
                sleepDepth = 0.82f,
            )
            InternalState.DEFENSIVE -> BodyExpressionModel(
                breathRate = 0.35f,
                breathDepth = 0.35f,
                pulseIntensity = 0.4f,
                brightness = 0.42f,
                contraction = 0.85f,
                instability = 0.28f,
                openness = 0.18f,
                sleepDepth = 0.25f,
            )
            InternalState.CURIOUS -> BodyExpressionModel(
                breathRate = 0.55f,
                breathDepth = 0.52f,
                pulseIntensity = 0.72f,
                brightness = 0.78f,
                contraction = 0.28f,
                instability = 0.18f,
                openness = 0.72f,
                sleepDepth = 0.14f,
            )
            InternalState.DISTURBED -> BodyExpressionModel(
                breathRate = 0.68f,
                breathDepth = 0.42f,
                pulseIntensity = 0.65f,
                brightness = 0.52f,
                contraction = 0.62f,
                instability = 0.55f,
                openness = 0.32f,
                sleepDepth = 0.12f,
            )
            InternalState.RECOVERING -> BodyExpressionModel(
                breathRate = 0.4f,
                breathDepth = 0.58f,
                pulseIntensity = 0.48f,
                brightness = 0.62f,
                contraction = 0.45f,
                instability = 0.22f,
                openness = 0.58f,
                sleepDepth = 0.28f,
            )
            InternalState.DORMANT -> BodyExpressionModel(
                breathRate = 0.12f,
                breathDepth = 0.55f,
                pulseIntensity = 0.22f,
                brightness = 0.28f,
                contraction = 0.25f,
                instability = 0.03f,
                openness = 0.4f,
                sleepDepth = 0.92f,
            )
            InternalState.ALERT -> BodyExpressionModel(
                breathRate = 0.62f,
                breathDepth = 0.48f,
                pulseIntensity = 0.75f,
                brightness = 0.68f,
                contraction = 0.4f,
                instability = 0.25f,
                openness = 0.62f,
                sleepDepth = 0.1f,
            )
        }
    }

    private fun applyDeviceModifiers(
        base: BodyExpressionModel,
        state: InternalState,
        env: EnvironmentalContext,
        hints: SomaticHints,
    ): BodyExpressionModel {
        var breathRate = base.breathRate
        var breathDepth = base.breathDepth
        var pulseIntensity = base.pulseIntensity
        var brightness = base.brightness
        var contraction = base.contraction
        var instability = base.instability
        var openness = base.openness
        var sleepDepth = base.sleepDepth

        val energy = env.energyRatio.coerceIn(0f, 1f)
        val thermal = env.thermalRatio.coerceIn(0f, 1f)
        val motion = env.motionEnergy.coerceIn(0f, 1f)

        // Low battery: dimmer, shallower breath (starvation physiology).
        if (energy < 0.28f) {
            val strain = 1f - (energy / 0.28f).coerceIn(0f, 1f)
            brightness *= 1f - 0.45f * strain
            breathDepth *= 1f - 0.35f * strain
            pulseIntensity *= 1f - 0.2f * strain
        }

        // Charging: smoother, slightly stronger pulse (replenishment).
        if (env.charging) {
            pulseIntensity = min(1f, pulseIntensity * 1.18f)
            instability *= 0.82f
            breathRate *= 0.92f
        }

        // Heat: instability and protective contraction.
        instability = min(1f, instability + thermal * 0.45f)
        contraction = min(1f, contraction + thermal * 0.22f)

        // Stillness over time deepens rest-like expression when already at rest.
        if (state == InternalState.RESTING || state == InternalState.DORMANT || state == InternalState.CALM) {
            val still = (hints.stillnessDurationSeconds / 120f).coerceIn(0f, 1f)
            sleepDepth = min(1f, sleepDepth + still * 0.18f)
            breathRate *= 1f - 0.12f * still
            breathDepth = min(1f, breathDepth + still * 0.1f)
        }

        // Motion briefly increases alertness / curiosity pulse.
        if (hints.motionAlertSecondsRemaining > 0.05f) {
            val alert = (hints.motionAlertSecondsRemaining / 3f).coerceIn(0f, 1f)
            pulseIntensity = min(1f, pulseIntensity + 0.22f * alert)
            breathRate = min(1f, breathRate + 0.12f * alert)
            brightness = min(1f, brightness + 0.08f * alert)
        }

        // Motion energy directly nudges openness and breath when not in defensive modes.
        if (state != InternalState.DEFENSIVE && state != InternalState.STRESSED) {
            openness = min(1f, openness + motion * 0.12f)
        }

        return BodyExpressionModel(
            breathRate = breathRate.coerceIn(0.05f, 1f),
            breathDepth = breathDepth.coerceIn(0.1f, 1f),
            pulseIntensity = pulseIntensity.coerceIn(0.05f, 1f),
            brightness = brightness.coerceIn(0.12f, 1f),
            contraction = contraction.coerceIn(0f, 1f),
            instability = instability.coerceIn(0f, 1f),
            openness = openness.coerceIn(0f, 1f),
            sleepDepth = sleepDepth.coerceIn(0f, 1f),
        )
    }
}
