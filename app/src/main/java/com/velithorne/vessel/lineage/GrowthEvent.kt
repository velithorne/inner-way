package com.velithorne.vessel.lineage

enum class GrowthEventType {
    CROWN_VISIBLE,
    LATERAL_EMERGED,
    RESERVE_CONTRACTED,
    RESERVE_EXPANDED,
    SHELL_THICKENED,
    CHAMBER_ENVELOPE,
    COHERENCE_IMPROVED,
    STAGE_ADVANCED,
    /** Leading morphology family changed (persisted affinity / selection). */
    BRANCH_LEAD_CHANGED,
    CUSTOM,
}

data class GrowthEvent(
    val id: Long,
    val timestampMillis: Long,
    val eventType: GrowthEventType,
    val affectedRegion: String,
    val magnitude: Float,
    val primaryDriver: String,
    val explanation: String,
    val occurredDuringOfflineCatchUp: Boolean,
)
