package com.velithorne.vessel

import android.app.Application
import com.velithorne.vessel.core.AppContainer

/**
 * Application entry. Holds [AppContainer] for dependency access.
 *
 * Future: inject [com.velithorne.vessel.domain.physiology.PhysiologyEngine],
 * Room DB, and navigation graph via Hilt or manual graph.
 */
class VesselApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
