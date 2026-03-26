package com.velithorne.vessel.util

object Smoothing {
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
