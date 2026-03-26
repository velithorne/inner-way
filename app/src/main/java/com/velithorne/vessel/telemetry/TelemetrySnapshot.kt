package com.velithorne.vessel.telemetry

/**
 * Normalized instant of device telemetry. Null fields mean unknown or unsupported
 * on this device/build (never crash the UI on absent data).
 *
 * Downstream (Phase 2+):
 * - [com.velithorne.vessel.domain.physiology.PhysiologyEngine] ingests successive snapshots
 *   to drive organ stress / homeostasis.
 * - [SpeciesState] / [OrganState]: compact projections for evolution + renderer.
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
    val lowMemoryFlag: Boolean?,
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
