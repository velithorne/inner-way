package com.velithorne.vessel.core

import android.app.Application
import com.velithorne.vessel.BuildConfig
import com.velithorne.vessel.data.LineageRepository
import com.velithorne.vessel.data.prefs.AppBuildStateStore
import com.velithorne.vessel.data.prefs.GrowthStateStore
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthCoordinator
import com.velithorne.vessel.growthtime.GrowthResetPolicy
import com.velithorne.vessel.growthtime.GrowthTimeCoordinator
import com.velithorne.vessel.growthtime.TimeTuning
import com.velithorne.vessel.morphogenesis.MorphogenesisEngine
import com.velithorne.vessel.physiology.PhysiologyEngine
import com.velithorne.vessel.renderer_seedpod.SeedPodRenderer
import com.velithorne.vessel.telemetry.TelemetryRepository

/**
 * Lightweight service locator.
 */
class AppContainer(app: Application) {

    val timeProvider: TimeProvider = TimeProvider()

    val telemetryRepository: TelemetryRepository = TelemetryRepository(
        application = app,
        timeProvider = timeProvider,
    )

    val physiologyEngine: PhysiologyEngine = PhysiologyEngine()

    val lineageRepository: LineageRepository = LineageRepository(app)

    val morphogenesisEngine: MorphogenesisEngine = MorphogenesisEngine()

    init {
        /** **Where specimen reset happens on build change:** morphogenesis prefs + temporal growth prefs + Room lineage. */
        GrowthResetPolicy.applyIfNewBuild(
            currentVersionCode = BuildConfig.VERSION_CODE,
            buildStore = AppBuildStateStore(app),
            growthStore = GrowthStateStore(app),
            morphogenesisEngine = morphogenesisEngine,
            lineageRepository = lineageRepository,
            tuning = TimeTuning(),
        )
    }

    val seedPodGrowthCoordinator: SeedPodGrowthCoordinator = SeedPodGrowthCoordinator(
        context = app,
        lineageRepository = lineageRepository,
    )
    val seedPodRenderer: SeedPodRenderer = SeedPodRenderer()

    val growthTimeCoordinator: GrowthTimeCoordinator = GrowthTimeCoordinator(
        context = app,
        morphogenesisEngine = morphogenesisEngine,
        appVersionCode = BuildConfig.VERSION_CODE,
    )
}
