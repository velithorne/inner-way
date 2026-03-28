package com.velithorne.vessel.config

import android.content.Context

private const val PREFS = "velithorne_dev_settings"
private const val KEY_FORCE_DEV_PROFILE = "force_dev_profile"
private const val KEY_PENDING_RESET = "pending_specimen_reset"
private const val KEY_DEV_EVOLUTION_SPEED = "dev_evolution_speed_multiplier"

/**
 * Debug-only developer toggles. No-op in release if checks are gated.
 */
class DevSettingsStore(context: Context) {
    private val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** When true, [ProfileResolver] uses DEV_SIMULATION even on non-debug builds (QA only). */
    var forceDevSimulationProfile: Boolean
        get() = p.getBoolean(KEY_FORCE_DEV_PROFILE, false)
        set(value) = p.edit().putBoolean(KEY_FORCE_DEV_PROFILE, value).apply()

    /** If set, next app foreground will clear lineage and reload specimen (see [TelemetryViewModel]). */
    var pendingSpecimenReset: Boolean
        get() = p.getBoolean(KEY_PENDING_RESET, false)
        set(value) = p.edit().putBoolean(KEY_PENDING_RESET, value).apply()

    fun clearPendingReset() {
        p.edit().putBoolean(KEY_PENDING_RESET, false).apply()
    }

    /**
     * Dev simulation only: multiplier on structural + visual evolution pacing (1 = default dev profile).
     * Persisted so testing sessions stay consistent across launches.
     */
    var devEvolutionSpeedMultiplier: Float
        get() = GrowthProfileScaler.clampMultiplier(p.getFloat(KEY_DEV_EVOLUTION_SPEED, 1f))
        set(value) {
            p.edit().putFloat(KEY_DEV_EVOLUTION_SPEED, GrowthProfileScaler.clampMultiplier(value)).apply()
        }
}
