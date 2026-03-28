package com.velithorne.vessel.telemetry

import android.os.Build
import com.velithorne.vessel.core.TimeProvider

/** Static / slow-changing device facts and monotonic uptime. */
class DeviceInfoProvider(
    private val timeProvider: TimeProvider,
) {
    val deviceModel: String = Build.MODEL?.ifBlank { null } ?: "Unknown"

    val androidVersion: String = Build.VERSION.RELEASE ?: "Unknown"

    fun uptimeMillis(): Long = timeProvider.uptimeMillis()
}
