package com.velithorne.innerway.body

import android.app.ActivityManager
import android.content.Context

/**
 * Nervous load: approximate strain from memory pressure (no root).
 */
class NervousSystem(
    private val context: Context,
) {

    data class Sample(
        val cognitivePressure: Float,
        val timestampMillis: Long,
    )

    suspend fun sample(): Sample {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        val usedRatio = if (info.totalMem > 0) {
            1f - (info.availMem.toFloat() / info.totalMem.toFloat())
        } else {
            0.3f
        }
        val low = if (info.lowMemory) 0.85f else usedRatio
        return Sample(cognitivePressure = low.coerceIn(0f, 1f), timestampMillis = System.currentTimeMillis())
    }
}
