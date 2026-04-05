package com.velithorne.innerway.body

import android.content.Context
import android.os.StatFs
import java.io.File

/**
 * Storage skeleton: free space as tissue capacity.
 */
class StorageSkeletonSystem(
    private val context: Context,
) {

    data class Sample(
        val freeRatio: Float,
        val timestampMillis: Long,
    )

    suspend fun sample(): Sample {
        val path: File = context.filesDir
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val available = stat.availableBlocksLong * blockSize
        val total = stat.blockCountLong * blockSize
        val ratio = if (total > 0) available.toFloat() / total.toFloat() else 0.5f
        return Sample(freeRatio = ratio.coerceIn(0f, 1f), timestampMillis = System.currentTimeMillis())
    }
}
