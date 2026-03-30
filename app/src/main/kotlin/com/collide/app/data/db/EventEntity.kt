package com.collide.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val inputFileName: String,
    val inputSize: Long,
    val baselineType: String,
    val baselineSize: Long,
    val winningSize: Long,
    val byteSavings: Long,
    val recipeSummary: String,
    val recipeJson: String,
    val verificationPassed: Boolean,
    val elapsedMs: Long,
    val notes: String = "",
    // Phase 2 additions
    val originalSha256: String = "",
    val reconstructedSha256: String = "",
    val verificationMethod: String = "BYTE_EQUALITY_ONLY",
    val transformedPayloadSize: Long = 0L,
    val backendCompressedSize: Long = 0L,
    val transformMetadataSize: Long = 0L,
    val containerHeaderSize: Long = 8L,
    val candidateIndex: Int = -1,
    val runConfigJson: String = "",
    val engineVersion: String = "1.0.0-phase1",
    val replayStatus: String = "PENDING"
)
