package com.velithorne.vessel.core

import android.os.SystemClock

/** Abstraction over time sources for testability and consistent “uptime” semantics. */
class TimeProvider {
    fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()

    fun uptimeMillis(): Long = SystemClock.uptimeMillis()

    fun currentTimeMillis(): Long = System.currentTimeMillis()
}
