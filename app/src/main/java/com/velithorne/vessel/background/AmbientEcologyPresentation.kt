package com.velithorne.vessel.background

import android.content.Context
import com.velithorne.vessel.BuildConfig
import com.velithorne.vessel.data.db.entity.AmbientEventEntity
import com.velithorne.vessel.data.db.entity.AmbientEcologyMetaEntity
import com.velithorne.vessel.model.AmbientEcologyDebugPanel
import com.velithorne.vessel.model.AmbientEcologyUiState

object AmbientEcologyPresentation {

    fun build(
        snapshots: List<EcologySnapshot>,
        samplesSinceOpen: Int,
        meta: AmbientEcologyMetaEntity?,
        lastEvent: AmbientEventEntity?,
        workScheduled: Boolean,
        tuning: BackgroundTuning = BackgroundTuning(),
    ): AmbientEcologyUiState {
        val acc = AmbientGrowthAccumulator.accumulate(snapshots, tuning)
        val driver = when (acc.dominantDriver) {
            "signal_mobile" -> "Recent ambient driver: mobile connectivity"
            "thermal" -> "Recent ambient driver: thermal samples"
            "charging_recovery" -> "Recent ambient driver: charging / recovery"
            "idle_coherence" -> "Recent ambient driver: calm idle stretches"
            "reserve_stress" -> "Recent ambient driver: low reserve periods"
            "archive" -> "Recent ambient driver: storage burden"
            "motion" -> "Recent ambient driver: motion-rich intervals"
            else -> "Recent ambient driver: mixed ecology"
        }
        val last = snapshots.maxOfOrNull { it.timestampMillis }
        val ago = if (last == null) "No samples yet" else {
            val s = (System.currentTimeMillis() - last) / 1000L
            when {
                s < 120 -> "Last ambient sample: just now"
                s < 3600 -> "Last ambient sample: ${s / 60}m ago"
                else -> "Last ambient sample: ${s / 3600}h ago"
            }
        }
        val workHint = if (workScheduled) {
            "Periodic ecology work is scheduled (coarse, battery-safe)"
        } else {
            "Periodic ecology work not yet observed — may enqueue after install or under Doze"
        }
        val debug = if (BuildConfig.DEBUG) {
            AmbientEcologyDebugPanel(
                lastSnapshotWriteMillis = meta?.lastSnapshotWriteMillis,
                lastAppliedSnapshotMillis = meta?.lastAppliedSnapshotMillis,
                lastAmbientEventLabel = lastEvent?.let { "${it.kind}: ${it.detail}" },
                periodicWorkScheduled = workScheduled,
            )
        } else null
        return AmbientEcologyUiState(
            vesselHintLine = "Ambient ecology active · background growth sampling (coarse snapshots)",
            dominantDriverLine = driver,
            lastSampleAgoLabel = ago,
            backgroundSamplesSinceOpen = samplesSinceOpen,
            workScheduledHint = workHint,
            debugPanel = debug,
        )
    }

    fun workScheduled(context: Context): Boolean =
        AmbientWorkStatus.isPeriodicEcologyScheduled(context)
}
