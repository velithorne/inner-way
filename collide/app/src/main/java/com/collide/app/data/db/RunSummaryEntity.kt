package com.collide.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "run_summaries")
data class RunSummaryEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val mode: String,
    val candidatesSeen: Int,
    val candidatesPruned: Int,
    val candidatesEvaluated: Int,
    val parseFailures: Int,
    val invalidStructureCount: Int,
    val notInterestingCount: Int,
    val savedEventCount: Int,
    val elapsedMs: Long
)
