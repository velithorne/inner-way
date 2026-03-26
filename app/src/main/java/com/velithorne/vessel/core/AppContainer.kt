package com.velithorne.vessel.core

import android.app.Application
import com.velithorne.vessel.physiology.PhysiologyEngine
import com.velithorne.vessel.telemetry.TelemetryRepository

/**
 * Lightweight service locator.
 *
 * Phase 3: add renderer-scoped presenters; Phase 4+: Room + evolution graph.
 */
class AppContainer(app: Application) {

    val timeProvider: TimeProvider = TimeProvider()

    val telemetryRepository: TelemetryRepository = TelemetryRepository(
        application = app,
        timeProvider = timeProvider,
    )

    /** Process-scoped to preserve EMA state across ticks. */
    val physiologyEngine: PhysiologyEngine = PhysiologyEngine()
}
