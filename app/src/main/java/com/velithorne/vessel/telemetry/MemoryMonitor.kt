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

        val lowMemory = try {
            activityManager?.let { am ->
                val info = ActivityManager.MemoryInfo()
                am.getMemoryInfo(info)
                info.lowMemory
            }
        } catch (_: Throwable) {
            null
        }

        val lowRamDevice = try {
            activityManager?.isLowRamDevice
        } catch (_: Throwable) {
            null
        }

        return MemoryReading(
            memoryClassMb = memoryClassMb,
            lowMemoryFlag = lowMemory,
            lowRamDevice = lowRamDevice,
        )
    }

    data class MemoryReading(
        val memoryClassMb: Int?,
        /** From [ActivityManager.MemoryInfo.lowMemory] when available. */
        val lowMemoryFlag: Boolean?,
        /** Tier flag from [ActivityManager.isLowRamDevice]. */
        val lowRamDevice: Boolean?,
    )
}
