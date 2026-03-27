package com.velithorne.vessel.config

import android.content.Context

/**
 * Resolves [SimulationMode] from build type and optional dev override.
 *
 * - Debug / development builds → [SimulationMode.DEV_SIMULATION] by default.
 * - Release builds → [SimulationMode.RELEASE_REALTIME] unless [DevSettingsStore.forceDevSimulationProfile].
 */
object ProfileResolver {

    fun resolveMode(context: Context): SimulationMode {
        val overrideDev = BuildEnvironment.devSimulationOverrideEnabled(context)
        if (overrideDev) return SimulationMode.DEV_SIMULATION
        return if (BuildEnvironment.isDebugBuild) {
            SimulationMode.DEV_SIMULATION
        } else {
            SimulationMode.RELEASE_REALTIME
        }
    }

    fun resolveProfile(context: Context): GrowthProfile = when (resolveMode(context)) {
        SimulationMode.DEV_SIMULATION -> DevSimulationProfile.build()
        SimulationMode.RELEASE_REALTIME -> ReleaseGrowthProfile.build()
    }
}
