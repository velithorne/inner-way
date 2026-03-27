package com.velithorne.vessel.core

import android.app.Application
import com.velithorne.vessel.BuildConfig
import com.velithorne.vessel.config.GrowthProfileProvider
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

    /** Resolved once — drives progression, branching, ambient, temporal growth. */
    val growthProfileProvider: GrowthProfileProvider = GrowthProfileProvider(application)

    private val profile get() = growthProfileProvider.profile

    val lineageRepository: LineageRepository = LineageRepository(
        context = application,
        branchingTuning = profile.branching,
        backgroundTuning = profile.background,
    )

    val morphogenesisEngine: MorphogenesisEngine = MorphogenesisEngine()

    init {
        /** **Where specimen reset happens on build change:** morphogenesis prefs + temporal growth prefs + Room lineage. */
        GrowthResetPolicy.applyIfNewBuild(
            currentVersionCode = BuildConfig.VERSION_CODE,
            buildStore = AppBuildStateStore(application),
            growthStore = GrowthStateStore(application),
            morphogenesisEngine = morphogenesisEngine,
            lineageRepository = lineageRepository,
            tuning = profile.time,
        )
        EcologyWorkScheduler.schedule(application, profile.background)
    }

    val seedPodGrowthCoordinator: SeedPodGrowthCoordinator = SeedPodGrowthCoordinator(
        context = application,
        lineageRepository = lineageRepository,
        profile = profile,
    )
    val seedPodRenderer: SeedPodRenderer = SeedPodRenderer()

    val growthTimeCoordinator: GrowthTimeCoordinator = GrowthTimeCoordinator(
        context = application,
        morphogenesisEngine = morphogenesisEngine,
        timeTuning = profile.time,
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

    /** Network capability changes — [com.velithorne.vessel.background.ConnectivityReceiver]. */
    fun recordConnectivityAmbientEvent() {
        runBlocking(Dispatchers.IO) {
            val specimenId = lineageRepository.ensureActiveSpecimenExists()
            val growth = lineageRepository.ensureSeedRowForSpecimen(specimenId)
            val telem = com.velithorne.vessel.telemetry.PassiveTelemetrySnapshotFactory.build(application, timeProvider)
            lineageRepository.insertAmbientEvent("NET", telem.networkType.name.lowercase())
            val snap = EcologySnapshot.fromTelemetryAndState(
                telem,
                growth,
                EcologySnapshotSourceType.CONNECTIVITY_EVENT,
                notes = "connectivity",
            )
            lineageRepository.insertEcologySnapshot(snap, force = true)
        }
    }
}
