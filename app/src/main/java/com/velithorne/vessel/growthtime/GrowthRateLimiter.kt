package com.velithorne.vessel.growthtime

import kotlin.math.abs

object GrowthRateLimiter {

    fun limitDeltaPerMinute(
        prev: Float,
        target: Float,
        maxDeltaPerMinute: Float,
        dtSec: Float,
    ): Float {
        val maxStep = maxDeltaPerMinute * (dtSec / 60f)
        val delta = target - prev
        return when {
            abs(delta) <= maxStep -> target
            delta > 0f -> prev + maxStep
            else -> prev - maxStep
        }
    }

    fun limitDeltaPerHour(
        prev: Float,
        target: Float,
        maxDeltaPerHour: Float,
        dtSec: Float,
    ): Float {
        val maxStep = maxDeltaPerHour * (dtSec / 3600f)
        val delta = target - prev
        return when {
            abs(delta) <= maxStep -> target
            delta > 0f -> prev + maxStep
            else -> prev - maxStep
        }
    }
}
