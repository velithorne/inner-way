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
    val notes: String = ""
)
