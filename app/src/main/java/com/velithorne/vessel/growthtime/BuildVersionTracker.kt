package com.velithorne.vessel.growthtime

import com.velithorne.vessel.data.prefs.AppBuildStateStore

/** Thin wrapper for version code tracking (APK identity). */
class BuildVersionTracker(
    private val store: AppBuildStateStore,
) {
    fun lastVersionCode(): Int = store.getLastVersionCode()
}
