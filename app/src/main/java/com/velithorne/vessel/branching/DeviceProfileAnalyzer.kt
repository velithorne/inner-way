package com.velithorne.vessel.branching

import com.velithorne.vessel.telemetry.NetworkTransport
import com.velithorne.vessel.telemetry.TelemetrySnapshot
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.min

object DeviceProfileAnalyzer {

    private const val BATTERY_COOL_C = 28f
    private const val BATTERY_WARM_C = 38f

    fun analyze(telemetry: TelemetrySnapshot): DeviceProfile {
        val memMb = telemetry.memoryClassMb ?: 256
        val lowMem = if (telemetry.lowMemoryFlag == true) 1f else 0f
        val lowRam = if (telemetry.lowRamDevice == true) 0.35f else 0f
        val hardwareTierNorm = (1f - (memMb.coerceIn(256, 8192) - 256f) / (8192f - 256f)).coerceIn(0f, 1f)
        val lowThermalHeadroom = (0.25f + (if (memMb < 384) 0.2f else 0f) + lowMem * 0.35f + lowRam).coerceIn(0f, 1f)

        val mobileHeavy = when (telemetry.networkType) {
            NetworkTransport.CELLULAR -> 0.55f
            NetworkTransport.WIFI -> 0.15f
            else -> 0.35f
        }
        val metered = if (telemetry.networkMetered == true) 0.25f else 0f
        val highSignalDependency = (mobileHeavy + metered).coerceIn(0f, 1f)

        val motion = (telemetry.motionIntensity ?: 0f).coerceIn(0f, 1f)
        val highMotionLife = (motion * 0.85f + min(1f, abs(telemetry.orientationPitchDeg ?: 0f) / 45f) * 0.15f).coerceIn(0f, 1f)

        val usedPct = telemetry.storageUsedPct
        val storageDense = when {
            usedPct == null -> 0.35f
            usedPct > 0.88f -> 0.95f
            usedPct > 0.65f -> 0.65f
            else -> 0.25f
        }
        val storageCap = telemetry.storageFreeBytes?.let { free ->
            val used = telemetry.storageUsedBytes
            when {
                used != null && used + free > 0L -> {
                    val total = used + free
                    (total.toFloat() / 512_000_000_000f).coerceIn(0f, 1f)
                }
                else -> storageDense
            }
        } ?: storageDense

        val charging = telemetry.isCharging == true
        val stableCharging = if (charging) 0.55f else 0.2f

        val batTemp = telemetry.batteryTempC
        val batteryThermalTrend = when {
            batTemp == null -> 0.35f
            else -> ((batTemp - BATTERY_COOL_C) / (BATTERY_WARM_C - BATTERY_COOL_C)).coerceIn(0f, 1f)
        }
        val thermalForHeadroom = batteryThermalTrend * 0.45f + (if (charging) 0.15f else 0f)

        val screenOn = telemetry.screenInteractive != false
        val motionLow = (telemetry.motionIntensity ?: 0.5f) < 0.18f
        val nocturnalUsageBias = ((if (!screenOn) 0.55f else 0.1f) + (if (motionLow) 0.25f else 0f)).coerceIn(0f, 1f)

        val upMs = telemetry.uptimeMillis.coerceAtLeast(1L)
        val uptimeRhythmTendency = (ln(upMs.toDouble() / 3_600_000.0) / ln(48.0)).toFloat().coerceIn(0f, 1f)

        var sensorScore = 0.25f
        if (telemetry.ambientLightLux != null) sensorScore += 0.25f
        if (telemetry.motionIntensity != null) sensorScore += 0.25f
        val highSensorRichness = sensorScore.coerceIn(0f, 1f)

        val lowMemoryTol = (if (memMb < 512) 0.45f else 0.15f) + lowMem * 0.4f

        return DeviceProfile(
            lowThermalHeadroom = (lowThermalHeadroom * 0.65f + thermalForHeadroom * 0.35f).coerceIn(0f, 1f),
            highSignalDependency = highSignalDependency,
            highMotionLife = highMotionLife,
            storageDenseProfile = storageDense,
            stableChargingProfile = stableCharging,
            highSensorRichness = highSensorRichness,
            lowMemoryPressureTolerance = lowMemoryTol.coerceIn(0f, 1f),
            hardwareTierNorm = hardwareTierNorm,
            storageCapacityClass = storageCap,
            batteryThermalTrend = batteryThermalTrend,
            nocturnalUsageBias = nocturnalUsageBias,
            uptimeRhythmTendency = uptimeRhythmTendency,
        )
    }
}
