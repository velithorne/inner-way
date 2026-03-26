package com.velithorne.vessel.util

object Smoothing {
    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t.coerceIn(0f, 1f)

    fun exponentialMovingAverage(
        previous: Float?,
        next: Float?,
        alpha: Float,
    ): Float? {
        if (next == null) return previous
        val p = previous
        return if (p == null) next else p + alpha * (next - p)
    }
}
