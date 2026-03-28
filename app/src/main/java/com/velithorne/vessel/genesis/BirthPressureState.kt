package com.velithorne.vessel.genesis

import com.velithorne.vessel.morphogenesis_core.GrowthPressureState
import com.velithorne.vessel.telemetry.TelemetrySnapshot

/**
 * Device-conditioned birth context — extends morphogenesis [GrowthPressureState] with live telemetry scalars.
 */
data class BirthPressureState(
    val growthPressure: GrowthPressureState,
    val isCharging: Boolean,
    val batteryFraction: Float,
    val thermalStressHint: Float,
    val networkStrainHint: Float,
    val storagePressureHint: Float,
    val motionHint: Float,
    val uptimeNormalizedHint: Float,
) {
    companion object {
        fun from(telem: TelemetrySnapshot, pressure: GrowthPressureState): BirthPressureState {
            val dayMs = 24L * 60L * 60L * 1000L
            val up = (telem.uptimeMillis.toFloat() / dayMs.toFloat()).coerceIn(0f, 1f)
            return BirthPressureState(
                growthPressure = pressure,
                isCharging = telem.isCharging == true,
                batteryFraction = (telem.batteryPct ?: 50f) / 100f,
                thermalStressHint = telem.batteryTempC?.let { (it / 45f).coerceIn(0f, 1f) } ?: 0.35f,
                networkStrainHint = if (telem.networkConnected == false) 0.65f else 0.12f,
                storagePressureHint = telem.storageUsedPct?.coerceIn(0f, 1f) ?: 0.5f,
                motionHint = telem.motionIntensity?.coerceIn(0f, 1f) ?: 0f,
                uptimeNormalizedHint = up,
            )
        }
    }
}
