package com.velithorne.vessel.model

/** Small Lineage / Vessel hints for ambient sampling. */
data class AmbientEcologyUiState(
    val vesselHintLine: String,
    val dominantDriverLine: String,
    val lastSampleAgoLabel: String,
    val backgroundSamplesSinceOpen: Int,
    val workScheduledHint: String,
    /** Non-null in debug builds — last snapshot / event / WorkManager for validation. */
    val debugPanel: AmbientEcologyDebugPanel? = null,
)

data class AmbientEcologyDebugPanel(
    val lastSnapshotWriteMillis: Long?,
    val lastAppliedSnapshotMillis: Long?,
    val lastAmbientEventLabel: String?,
    val periodicWorkScheduled: Boolean,
)
