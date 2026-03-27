package com.velithorne.vessel.config

import com.velithorne.vessel.BuildConfig

/**
 * Build-time flags — single place to read [BuildConfig.DEBUG] etc.
 */
object BuildEnvironment {

    val isDebugBuild: Boolean get() = BuildConfig.DEBUG

    /** Optional manual override for QA (set in [DevSettingsStore]). */
    fun devSimulationOverrideEnabled(context: android.content.Context): Boolean =
        DevSettingsStore(context).forceDevSimulationProfile
}
