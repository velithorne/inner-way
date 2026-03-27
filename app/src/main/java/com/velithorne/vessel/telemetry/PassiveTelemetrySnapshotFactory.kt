package com.velithorne.vessel.telemetry

import android.app.Application
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.velithorne.vessel.core.TimeProvider

/**
 * Builds a [TelemetrySnapshot] without registering sensors — safe for WorkManager / background.
 * Motion and orientation are null; other fields match foreground aggregation where possible.
 */
object PassiveTelemetrySnapshotFactory {

    fun build(
        app: Application,
        timeProvider: TimeProvider,
    ): TelemetrySnapshot {
        val battery = BatteryMonitor(app).read()
        val storage = StorageMonitor(app).read()
        val memory = MemoryMonitor(app).read()
        val network = NetworkMonitor(app).read()
        val deviceInfo = DeviceInfoProvider(timeProvider)
        val pm = ContextCompat.getSystemService(app, PowerManager::class.java)
        val screenOn = try {
            pm?.isInteractive
        } catch (_: Throwable) {
            null
        }
        return TelemetrySnapshot(
            timestampMillis = timeProvider.currentTimeMillis(),
            batteryPct = battery.batteryPct,
            isCharging = battery.isCharging,
            batteryTempC = battery.batteryTempC,
            powerSaveEnabled = battery.powerSaveEnabled,
            storageUsedBytes = storage.usedBytes,
            storageFreeBytes = storage.freeBytes,
            storageUsedPct = storage.usedPct,
            memoryClassMb = memory.memoryClassMb,
            lowMemoryFlag = memory.lowMemoryFlag,
            lowRamDevice = memory.lowRamDevice,
            networkConnected = network.connected,
            networkType = network.transport,
            networkMetered = network.metered,
            motionIntensity = null,
            orientationPitchDeg = null,
            orientationRollDeg = null,
            ambientLightLux = null,
            screenInteractive = screenOn,
            uptimeMillis = deviceInfo.uptimeMillis(),
            deviceModel = deviceInfo.deviceModel,
            androidVersion = deviceInfo.androidVersion,
        )
    }
}
