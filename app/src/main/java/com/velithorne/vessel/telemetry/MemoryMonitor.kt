package com.velithorne.vessel.telemetry

import android.app.ActivityManager
import android.content.Context
import androidx.core.content.ContextCompat

/**
 * Android “memory class” and a conservative low-memory hint.
 * Does not fabricate CPU load.
 */
class MemoryMonitor(
    private val appContext: Context,
) {
    private val activityManager: ActivityManager? =
        ContextCompat.getSystemService(appContext, ActivityManager::class.java)

    fun read(): MemoryReading {
        val memoryClassMb = try {
            activityManager?.memoryClass
        } catch (_: Throwable) {
            null
        }

        val lowRam = try {
            activityManager?.isLowRamDevice
        } catch (_: Throwable) {
            null
        }

        return MemoryReading(
            memoryClassMb = memoryClassMb,
            lowMemoryFlag = lowRam,
        )
    }

    data class MemoryReading(
        val memoryClassMb: Int?,
        /** Devices in low-RAM mode; not the same as “critical RAM right now”. */
        val lowMemoryFlag: Boolean?,
    )
}
