package com.velithorne.vessel.telemetry

import android.content.Context
import android.os.Environment
import android.os.StatFs

/** Internal shared storage stats (app-accessible data partition). */
class StorageMonitor(
    private val appContext: Context,
) {
    fun read(): StorageReading {
        return try {
            val dir = appContext.filesDir
                ?: Environment.getDataDirectory()
            val stat = StatFs(dir.path)
            val blockSize = stat.blockSizeLong
            val total = stat.blockCountLong * blockSize
            val available = stat.availableBlocksLong * blockSize
            val used = (total - available).coerceAtLeast(0L)
            val usedPct = if (total > 0) {
                (used.toFloat() / total.toFloat()) * 100f
            } else {
                null
            }
            StorageReading(
                usedBytes = used,
                freeBytes = available.coerceAtLeast(0L),
                usedPct = usedPct,
            )
        } catch (_: Throwable) {
            StorageReading(null, null, null)
        }
    }

    data class StorageReading(
        val usedBytes: Long?,
        val freeBytes: Long?,
        val usedPct: Float?,
    )
}
