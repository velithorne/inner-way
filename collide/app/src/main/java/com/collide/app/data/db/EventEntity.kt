package com.collide.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val mode: String,
    val inputLabelA: String,
    val inputLabelB: String?,
    val inputProfileA: String,
    val inputProfileB: String?,
    val eventType: String,
    val collisionRecipeJson: String,
    val candidatePreview: String,
    val detectorSummaryJson: String,
    val replayable: Boolean,
    val notes: String,
    val replayStatus: String = "NOT_REPLAYED"
)
