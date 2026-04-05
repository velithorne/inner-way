package com.velithorne.innerway.body

import android.content.Context
import android.os.PowerManager

/**
 * Thermal body: uses PowerManager thermal status where available (API 29+).
 */
class ThermalBodySystem(
    private val context: Context,
) {

    data class Sample(
        val thermalStressRatio: Float,
        val timestampMillis: Long,
    )

    suspend fun sample(): Sample {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val stress = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            when (pm.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE,
                PowerManager.THERMAL_STATUS_LIGHT,
                -> 0.2f
                PowerManager.THERMAL_STATUS_MODERATE -> 0.55f
                PowerManager.THERMAL_STATUS_SEVERE -> 0.78f
                PowerManager.THERMAL_STATUS_CRITICAL,
                PowerManager.THERMAL_STATUS_EMERGENCY,
                PowerManager.THERMAL_STATUS_SHUTDOWN,
                -> 0.95f
                else -> 0.35f
            }
        } else {
            0.35f
        }
        return Sample(thermalStressRatio = stress.coerceIn(0f, 1f), timestampMillis = System.currentTimeMillis())
    }
}
