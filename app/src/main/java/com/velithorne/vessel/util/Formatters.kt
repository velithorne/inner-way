package com.velithorne.vessel.util

import java.util.Locale
import java.util.concurrent.TimeUnit

object Formatters {

    fun formatPercent(value: Float?, decimals: Int = 1): String {
        if (value == null) return unavailable()
        return "%.${decimals}f%%".format(Locale.US, value)
    }

    fun formatDegrees(value: Float?, decimals: Int = 1): String {
        if (value == null) return unavailable()
        return "%.${decimals}f°".format(Locale.US, value)
    }

    fun formatMotionIntensity(value: Float?, decimals: Int = 3): String {
        if (value == null) return unavailable()
        return "%.${decimals}f".format(Locale.US, value)
    }

    fun formatLux(value: Float?): String {
        if (value == null) return unavailable()
        return "%.0f lx".format(Locale.US, value)
    }

    fun formatCelsius(value: Float?): String {
        if (value == null) return unavailable()
        return "%.1f °C".format(Locale.US, value)
    }

    fun formatBytes(bytes: Long?): String {
        if (bytes == null) return unavailable()
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(Locale.US, kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(Locale.US, mb)
        val gb = mb / 1024.0
        return "%.2f GB".format(Locale.US, gb)
    }

    fun formatDurationMs(ms: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(ms)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        return if (hours > 0) {
            String.format(Locale.US, "%dh %02dm %02ds", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%dm %02ds", minutes, seconds)
        }
    }

    fun formatTimestampMillis(ts: Long): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
        return sdf.format(java.util.Date(ts))
    }

    /** Normalized scalar for physiology bars (0..1). */
    fun formatUnitInterval(value: Float, decimals: Int = 2): String {
        return "%.${decimals}f".format(Locale.US, value.coerceIn(0f, 1f))
    }

    fun unavailable(): String = "Unavailable"

    fun yesNo(value: Boolean?): String = when (value) {
        true -> "Yes"
        false -> "No"
        null -> unavailable()
    }
}
