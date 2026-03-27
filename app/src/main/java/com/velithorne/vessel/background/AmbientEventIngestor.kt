package com.velithorne.vessel.background

import android.app.Application
import com.velithorne.vessel.data.LineageRepository
import com.velithorne.vessel.telemetry.TelemetryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Foreground lifecycle hooks — BG snapshot uses live telemetry; FG resets sample counter.
 */
class AmbientEventIngestor(
    private val app: Application,
    private val lineageRepository: LineageRepository,
    private val telemetryRepository: TelemetryRepository,
) {

    suspend fun onAppForeground() = withContext(Dispatchers.IO) {
        val id = lineageRepository.activeSpecimenIdOrNull ?: return@withContext
        lineageRepository.insertAmbientEvent("FG", "foreground")
        lineageRepository.resetSamplesSinceOpen(id)
    }

    suspend fun onAppBackground() = withContext(Dispatchers.IO) {
        lineageRepository.insertAmbientEvent("BG", "background")
        val specimenId = lineageRepository.ensureActiveSpecimenExists()
        val growth = lineageRepository.ensureSeedRowForSpecimen(specimenId)
        val telem = telemetryRepository.snapshot.value
        val snap = EcologySnapshot.fromTelemetryAndState(
            telem,
            growth,
            EcologySnapshotSourceType.APP_BACKGROUND,
        )
        lineageRepository.insertEcologySnapshot(snap, force = true)
    }
}
