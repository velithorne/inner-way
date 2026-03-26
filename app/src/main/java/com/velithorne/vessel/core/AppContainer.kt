package com.velithorne.vessel.core

import android.app.Application
import com.velithorne.vessel.BuildConfig
import com.velithorne.vessel.growthtime.GrowthTimeCoordinator
import com.velithorne.vessel.morphogenesis.MorphogenesisEngine
import com.velithorne.vessel.physiology.PhysiologyEngine
import com.velithorne.vessel.renderer.VesselRenderer
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

    /** Telemetry → scene mapping (stateless); animation state lives in composables. */
    val vesselRenderer: VesselRenderer = VesselRenderer()

    val morphogenesisEngine: MorphogenesisEngine = MorphogenesisEngine()

    val growthTimeCoordinator: GrowthTimeCoordinator = GrowthTimeCoordinator(
        context = app,
        morphogenesisEngine = morphogenesisEngine,
        appVersionCode = BuildConfig.VERSION_CODE,
    )
}
