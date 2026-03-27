package com.velithorne.vessel.branching

import com.velithorne.vessel.telemetry.NetworkTransport
import com.velithorne.vessel.telemetry.TelemetrySnapshot
import kotlin.math.abs
import kotlin.math.min

object DeviceProfileAnalyzer {

    fun analyze(telemetry: TelemetrySnapshot): DeviceProfile {
        val memMb = telemetry.memoryClassMb ?: 256
        val lowMem = if (telemetry.lowMemoryFlag == true) 1f else 0f
        val lowRam = if (telemetry.lowRamDevice == true) 0.35f else 0f
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

        val charging = telemetry.isCharging == true
        val stableCharging = if (charging) 0.55f else 0.2f

        var sensorScore = 0.25f
        if (telemetry.ambientLightLux != null) sensorScore += 0.25f
        if (telemetry.motionIntensity != null) sensorScore += 0.25f
        val highSensorRichness = sensorScore.coerceIn(0f, 1f)

        val lowMemoryTol = (if (memMb < 512) 0.45f else 0.15f) + lowMem * 0.4f

        return DeviceProfile(
            lowThermalHeadroom = lowThermalHeadroom,
            highSignalDependency = highSignalDependency,
            highMotionLife = highMotionLife,
            storageDenseProfile = storageDense,
            stableChargingProfile = stableCharging,
            highSensorRichness = highSensorRichness,
            lowMemoryPressureTolerance = lowMemoryTol.coerceIn(0f, 1f),
        )
    }
}
