package com.velithorne.vessel.config

import android.app.Application

/**
 * Process-scoped access to the active [GrowthProfile] — resolved once from [ProfileResolver].
 */
class GrowthProfileProvider(
    application: Application,
) {
    private val app = application.applicationContext

    val profile: GrowthProfile by lazy { ProfileResolver.resolveProfile(app) }

    val mode: SimulationMode get() = profile.mode
}
