package com.velithorne.vessel.growthtime

/**
 * Wall-clock helpers for growth stepping (not frame-based).
 */
object GrowthClock {

    fun nowMillis(): Long = System.currentTimeMillis()

    fun elapsedSince(lastWallTimeMs: Long, now: Long = nowMillis()): Long =
        (now - lastWallTimeMs).coerceAtLeast(0L)

    fun secondsFromMillis(ms: Long): Float = ms / 1000f
}
