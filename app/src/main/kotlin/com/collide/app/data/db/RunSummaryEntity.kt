package com.collide.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "run_summaries")
data class RunSummaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val fileName: String,
    val inputSize: Long,
    val runMode: String,
    val candidatesSeen: Int,
    val candidatesPruned: Int,
    val candidatesEvaluated: Int,
    val exactnessFailures: Int,
    val noGainCount: Int,
    val winnerCount: Int,
    val elapsedMs: Long
)
