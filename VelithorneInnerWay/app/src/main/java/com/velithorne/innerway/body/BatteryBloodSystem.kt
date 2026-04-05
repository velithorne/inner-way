package com.velithorne.innerway.body

import android.content.Context
import android.os.BatteryManager

/**
 * Energy / blood: maps battery state to organism vitality.
 */
class BatteryBloodSystem(
    private val context: Context,
) {

    data class Sample(
        val energyRatio: Float,
        val charging: Boolean,
        val timestampMillis: Long,
    )

    suspend fun sample(): Sample {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).takeIf { it >= 0 }
        val ratio = (level ?: 50) / 100f
        val status = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        return Sample(
            energyRatio = ratio.coerceIn(0f, 1f),
            charging = charging,
            timestampMillis = System.currentTimeMillis(),
        )
    }
}
