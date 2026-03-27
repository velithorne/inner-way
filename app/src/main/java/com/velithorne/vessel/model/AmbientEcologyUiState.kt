package com.velithorne.vessel.model

/** Small Lineage / Vessel hints for ambient sampling. */
data class AmbientEcologyUiState(
    val vesselHintLine: String,
    val dominantDriverLine: String,
    val lastSampleAgoLabel: String,
    val backgroundSamplesSinceOpen: Int,
    val workScheduledHint: String,
)
