package com.velithorne.vessel.branching

/**
 * Stable, explainable device/ecology tags — derived from specs + behavior, not identity.
 */
data class DeviceProfile(
    val lowThermalHeadroom: Float,
    val highSignalDependency: Float,
    val highMotionLife: Float,
    val storageDenseProfile: Float,
    val stableChargingProfile: Float,
    val highSensorRichness: Float,
    val lowMemoryPressureTolerance: Float,
) {
    companion object {
        /** Mid priors when only ecology/history is available (e.g. lineage screen without live telemetry). */
        fun neutral(): DeviceProfile = DeviceProfile(
            lowThermalHeadroom = 0.4f,
            highSignalDependency = 0.4f,
            highMotionLife = 0.4f,
            storageDenseProfile = 0.4f,
            stableChargingProfile = 0.35f,
            highSensorRichness = 0.4f,
            lowMemoryPressureTolerance = 0.35f,
        )
    }
}
