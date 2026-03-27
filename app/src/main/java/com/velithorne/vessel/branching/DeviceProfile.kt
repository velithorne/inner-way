package com.velithorne.vessel.branching

/**
 * Stable, explainable device/ecology tags — derived from specs + behavior, not identity.
 *
 * [hardwareTierNorm] / [storageCapacityClass] summarize class tiers for branching influence.
 * [batteryThermalTrend] rises when battery reports warm/hot vs cool (charging + temp).
 * [nocturnalUsageBias] is a weak “quiet / dark hours” proxy from screen + motion (not clock surveillance).
 * [uptimeRhythmTendency] captures long-session vs fresh-boot feel from uptime only.
 */
data class DeviceProfile(
    val lowThermalHeadroom: Float,
    val highSignalDependency: Float,
    val highMotionLife: Float,
    val storageDenseProfile: Float,
    val stableChargingProfile: Float,
    val highSensorRichness: Float,
    val lowMemoryPressureTolerance: Float,
    /** 0..1 — lower memory class / tighter headroom reads as more constrained hardware. */
    val hardwareTierNorm: Float,
    /** 0..1 — fuller storage / larger used fraction reads as denser archive ecology. */
    val storageCapacityClass: Float,
    /** 0..1 — thermal load from battery temperature + charging. */
    val batteryThermalTrend: Float,
    /** 0..1 — weak proxy: inactive screen + low motion suggests rest/night-style rhythm. */
    val nocturnalUsageBias: Float,
    /** 0..1 — long continuous uptime suggests sustained-use rhythm. */
    val uptimeRhythmTendency: Float,
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
            hardwareTierNorm = 0.45f,
            storageCapacityClass = 0.45f,
            batteryThermalTrend = 0.35f,
            nocturnalUsageBias = 0.3f,
            uptimeRhythmTendency = 0.35f,
        )
    }
}
