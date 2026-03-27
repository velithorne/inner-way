package com.velithorne.vessel.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.velithorne.vessel.VesselApplication
import com.velithorne.vessel.telemetry.PassiveTelemetrySnapshotFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Periodic coarse ecology sample — **no sensors**, battery-safe. See [BackgroundPolicy].
 */
class EcologyWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = applicationContext as? VesselApplication ?: return@withContext Result.success()
        if (!BackgroundPolicy.shouldRunPeriodicWork(applicationContext)) return@withContext Result.success()
        val container = app.appContainer
        val telem = PassiveTelemetrySnapshotFactory.build(app, container.timeProvider)
        val specimenId = container.lineageRepository.ensureActiveSpecimenExists()
        val growth = container.lineageRepository.ensureSeedRowForSpecimen(specimenId)
        val snap = EcologySnapshot.fromTelemetryAndState(
            telem,
            growth,
            EcologySnapshotSourceType.PERIODIC_WORK,
            notes = "work",
        )
        container.lineageRepository.insertEcologySnapshot(snap, force = true)
        val meta = container.lineageRepository.getAmbientEcologyMeta(specimenId)
            ?: com.velithorne.vessel.data.db.entity.AmbientEcologyMetaEntity(specimenId = specimenId)
        container.lineageRepository.upsertAmbientMeta(
            meta.copy(workLastScheduledMillis = System.currentTimeMillis()),
        )
        Result.success()
    }
}
