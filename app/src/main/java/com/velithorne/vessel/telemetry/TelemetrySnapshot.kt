package com.velithorne.vessel.telemetry

/**
 * Normalized instant of device telemetry. Null fields mean unknown or unsupported
 * on this device/build (never crash the UI on absent data).
 *
 * Downstream (Phase 2+):
 * - [com.velithorne.vessel.physiology.PhysiologyEngine] ingests successive snapshots
 *   to drive organ stress / homeostasis.
 * - [com.velithorne.vessel.physiology.SpeciesState] / [com.velithorne.vessel.physiology.OrganState]
 */
data class TelemetrySnapshot(
    val timestampMillis: Long,
    val batteryPct: Float?,
    val isCharging: Boolean?,
    val batteryTempC: Float?,
    val powerSaveEnabled: Boolean?,
    val storageUsedBytes: Long?,
    val storageFreeBytes: Long?,
    val storageUsedPct: Float?,
    val memoryClassMb: Int?,
    /** From [android.app.ActivityManager.MemoryInfo.lowMemory] when readable. */
    val lowMemoryFlag: Boolean?,
    /** From [android.app.ActivityManager.isLowRamDevice]. */
    val lowRamDevice: Boolean?,
    val networkConnected: Boolean?,
    val networkType: NetworkTransport,
    val networkMetered: Boolean?,
    /** Unitless accelerometer-derived dynamic component; smoothed in [TelemetryRepository]. */
    val motionIntensity: Float?,
    val orientationPitchDeg: Float?,
    val orientationRollDeg: Float?,
    val ambientLightLux: Float?,
    val screenInteractive: Boolean?,
    val uptimeMillis: Long,
    val deviceModel: String,
    val androidVersion: String,
)

enum class NetworkTransport {
    WIFI,
    CELLULAR,
    ETHERNET,
    VPN,
    BLUETOOTH,
    NONE,
    OTHER,
}
