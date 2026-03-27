package com.velithorne.vessel.background

import com.velithorne.vessel.model.AmbientEcologyUiState

object AmbientEcologyPresentation {

    fun build(
        snapshots: List<EcologySnapshot>,
        samplesSinceOpen: Int,
        tuning: BackgroundTuning = BackgroundTuning(),
    ): AmbientEcologyUiState {
        val acc = AmbientGrowthAccumulator.accumulate(snapshots, tuning)
        val driver = when (acc.dominantDriver) {
            "signal_mobile" -> "Recent ambient driver: mobile connectivity"
            "thermal" -> "Recent ambient driver: thermal samples"
            "charging_recovery" -> "Recent ambient driver: charging / recovery"
            "reserve_stress" -> "Recent ambient driver: low reserve periods"
            "archive" -> "Recent ambient driver: storage burden"
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
        return AmbientEcologyUiState(
            vesselHintLine = "Background growth sampling enabled · coarse ecology snapshots",
            dominantDriverLine = driver,
            lastSampleAgoLabel = ago,
            backgroundSamplesSinceOpen = samplesSinceOpen,
            workScheduledHint = "Periodic WorkManager ecology (battery-safe, inexact)",
        )
    }
}
