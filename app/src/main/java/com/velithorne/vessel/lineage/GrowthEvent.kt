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
    /** Branch readiness crossed the visibility threshold (post–chamber mature). Append-only for Room ordinals. */
    BRANCH_READINESS_UNLOCKED,
    /** A morphology family’s affinity rose meaningfully vs last persist. */
    BRANCH_TENDENCY_STRENGTHENED,
    /** Background ecology snapshots nudged adaptation / lineage (offline accumulation). */
    AMBIENT_ECOLOGY_INFLUENCE,
    /** Branch morphology became visibly distinct on the Vessel (readiness + stage gate). */
    BRANCH_VISUAL_APPARENT,
    /** Visible family traits strengthened (crown / lateral / shell / reserve emphasis). */
    BRANCH_VISUAL_TRAIT_REINFORCED,
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
