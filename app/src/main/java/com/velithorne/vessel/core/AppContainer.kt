package com.velithorne.vessel.core

import android.app.Application
import com.velithorne.vessel.BuildConfig
import com.velithorne.vessel.background.AmbientEventIngestor
import com.velithorne.vessel.background.EcologySnapshot
import com.velithorne.vessel.background.EcologySnapshotSourceType
import com.velithorne.vessel.background.EcologySnapshotStore
import com.velithorne.vessel.background.EcologyWorkScheduler
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Lightweight service locator.
 */
class AppContainer(
    private val application: Application,
) {

    val timeProvider: TimeProvider = TimeProvider()

    val telemetryRepository: TelemetryRepository = TelemetryRepository(
        application = application,
        timeProvider = timeProvider,
    )

    val physiologyEngine: PhysiologyEngine = PhysiologyEngine()

    val lineageRepository: LineageRepository = LineageRepository(application)

    val morphogenesisEngine: MorphogenesisEngine = MorphogenesisEngine()

    init {
        /** **Where specimen reset happens on build change:** morphogenesis prefs + temporal growth prefs + Room lineage. */
        GrowthResetPolicy.applyIfNewBuild(
            currentVersionCode = BuildConfig.VERSION_CODE,
            buildStore = AppBuildStateStore(application),
            growthStore = GrowthStateStore(application),
            morphogenesisEngine = morphogenesisEngine,
            lineageRepository = lineageRepository,
            tuning = TimeTuning(),
        )
        EcologyWorkScheduler.schedule(application)
    }

    val seedPodGrowthCoordinator: SeedPodGrowthCoordinator = SeedPodGrowthCoordinator(
        context = application,
        lineageRepository = lineageRepository,
    )
    val seedPodRenderer: SeedPodRenderer = SeedPodRenderer()

    val growthTimeCoordinator: GrowthTimeCoordinator = GrowthTimeCoordinator(
        context = application,
        morphogenesisEngine = morphogenesisEngine,
        appVersionCode = BuildConfig.VERSION_CODE,
    )

    val ecologySnapshotStore: EcologySnapshotStore = EcologySnapshotStore(lineageRepository)

    val ambientEventIngestor: AmbientEventIngestor = AmbientEventIngestor(
        app = application,
        lineageRepository = lineageRepository,
        telemetryRepository = telemetryRepository,
    )

    /** Charging plug events — called from [com.velithorne.vessel.background.ChargingReceiver]. */
    fun recordChargingAmbientEvent(isConnected: Boolean) {
        runBlocking(Dispatchers.IO) {
            val specimenId = lineageRepository.ensureActiveSpecimenExists()
            val growth = lineageRepository.ensureSeedRowForSpecimen(specimenId)
            val telem = com.velithorne.vessel.telemetry.PassiveTelemetrySnapshotFactory.build(application, timeProvider)
            lineageRepository.insertAmbientEvent("PWR", if (isConnected) "connected" else "disconnected")
            val snap = EcologySnapshot.fromTelemetryAndState(
                telem,
                growth,
                EcologySnapshotSourceType.CHARGING_EVENT,
                notes = "power",
            )
            lineageRepository.insertEcologySnapshot(snap, force = true)
        }
    }
}
