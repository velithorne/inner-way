package com.collide.app.domain.model

/**
 * A meaningful collision event that passed at least one detector threshold.
 * This is the unit of discovery in COLLIDE Phase 1.
 */
data class SavedEvent(
    val id: String,
    val timestamp: Long,
    val mode: CollisionMode,
    val inputLabelA: String,
    val inputLabelB: String?,
    val inputProfileA: String,
    val inputProfileB: String?,
    val eventType: EventType,
    val collisionRecipeJson: String,
    val candidatePreview: String,
    val detectorSummaryJson: String,
    val replayable: Boolean,
    val notes: String,
    val replayStatus: ReplayStatus = ReplayStatus.NOT_REPLAYED
)

enum class ReplayStatus {
    NOT_REPLAYED,
    REPLAY_MATCHED,
    REPLAY_DIFFERED,
    REPLAY_FAILED
}
