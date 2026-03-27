package com.velithorne.vessel.model

import com.velithorne.vessel.branching.DeviceProfile

/**
 * UI-friendly snapshot of device ecology tags (mirrors [DeviceProfile] for display if needed).
 */
data class DeviceMorphProfile(
    val lowThermalHeadroom: Float,
    val highSignalDependency: Float,
    val highMotionLife: Float,
    val storageDense: Float,
    val stableCharging: Float,
    val sensorRichness: Float,
    val memoryPressureSensitivity: Float,
) {
    companion object {
        fun from(p: DeviceProfile) = DeviceMorphProfile(
            lowThermalHeadroom = p.lowThermalHeadroom,
            highSignalDependency = p.highSignalDependency,
            highMotionLife = p.highMotionLife,
            storageDense = p.storageDenseProfile,
            stableCharging = p.stableChargingProfile,
            sensorRichness = p.highSensorRichness,
            memoryPressureSensitivity = p.lowMemoryPressureTolerance,
        )
    }
}
