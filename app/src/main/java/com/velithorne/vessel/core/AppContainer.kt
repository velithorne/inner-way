package com.velithorne.vessel.core

import android.app.Application
import com.velithorne.vessel.telemetry.TelemetryRepository

/**
 * Lightweight service locator for Phase 1.
 *
 * Integration hooks (TODO — Phase 2+):
 * - [Constants.FuturePhases.PHYSIOLOGY_MODULE]: wire [TelemetryRepository.snapshot] → PhysiologyEngine
 * - Organ mapping: SpeciesState / OrganState derived from physiology deltas
 * - [Constants.FuturePhases.EVOLUTION_MODULE]: consume long-running fitness signals
 * - [Constants.FuturePhases.PERSISTENCE_MODULE]: Room DAOs / DataStore for snapshots
 * - [Constants.FuturePhases.RENDERER_MODULE]: read-only bind to latest vessel state
 */
class AppContainer(app: Application) {

    val timeProvider: TimeProvider = TimeProvider()

    val telemetryRepository: TelemetryRepository = TelemetryRepository(
        application = app,
        timeProvider = timeProvider,
    )
}
