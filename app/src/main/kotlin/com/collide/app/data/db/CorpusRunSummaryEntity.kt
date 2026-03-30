package com.collide.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "corpus_run_summaries")
data class CorpusRunSummaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val runMode: String,
    val baselineStrategy: String,
    val totalFixtures: Int,
    val fixturesWithWins: Int,
    val fixturesWithoutWins: Int,
    val totalWinners: Int,
    val totalCandidatesEvaluated: Int,
    val totalElapsedMs: Long,
    val fixtureResultsJson: String  // JSON serialisation of the full FixtureResult list
)
