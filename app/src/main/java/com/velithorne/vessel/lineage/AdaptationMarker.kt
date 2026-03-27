package com.velithorne.vessel.lineage

enum class AdaptationKind {
    THERMAL,
    SIGNAL,
    NEURAL,
    RECOVERY,
    RESERVE,
    ARCHIVE,
}

data class AdaptationMarker(
    val kind: AdaptationKind,
    val accumulatedIntensity: Float,
    val lastTriggeredAtMillis: Long,
    val visibleBiasApplied: Float,
    val explanationLabel: String,
)
