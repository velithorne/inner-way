package com.velithorne.vessel.telemetry

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat

/**
 * Reads battery telemetry from the sticky [Intent.ACTION_BATTERY_CHANGED] broadcast.
 * No runtime permission required for that broadcast.
 */
class BatteryMonitor(
    private val appContext: Context,
) {
    private val powerManager: PowerManager? =
        ContextCompat.getSystemService(appContext, PowerManager::class.java)

    fun read(): BatteryReading {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(null, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            appContext.registerReceiver(null, filter)
        } ?: return BatteryReading(null, null, null, null)

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val pct = if (level >= 0 && scale > 0) {
            (level * 100f) / scale.toFloat()
        } else {
            null
        }

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING,
            BatteryManager.BATTERY_STATUS_FULL,
                -> true

            BatteryManager.BATTERY_STATUS_DISCHARGING,
            BatteryManager.BATTERY_STATUS_NOT_CHARGING,
                -> false

            else -> null
        }

        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val tempC = if (tempTenths != Int.MIN_VALUE) {
            tempTenths / 10f
        } else {
            null
        }

        val powerSave = if (Build.VERSION.SDK_INT >= 21) {
            powerManager?.isPowerSaveMode
        } else {
            null
        }

        return BatteryReading(
            batteryPct = pct,
            isCharging = charging,
            batteryTempC = tempC,
            powerSaveEnabled = powerSave,
        )
    }

    data class BatteryReading(
        val batteryPct: Float?,
        val isCharging: Boolean?,
        val batteryTempC: Float?,
        val powerSaveEnabled: Boolean?,
    )
}
