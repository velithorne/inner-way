package com.collide.app.domain.model

/**
 * Honest accounting of a collision run. All counts are tracked explicitly.
 * Failed and uninteresting volume is never hidden.
 */
data class RunStats(
    val candidatesSeen: Int = 0,
    val candidatesPrunedPreEval: Int = 0,
    val candidatesEvaluated: Int = 0,
    val parseFailures: Int = 0,
    val invalidStructureCount: Int = 0,
    val notInterestingCount: Int = 0,
    val savedEventCount: Int = 0,
    val evaluationErrors: Int = 0,
    val elapsedMs: Long = 0L,
    val status: RunStatus = RunStatus.IDLE
)

enum class RunStatus {
    IDLE,
    RUNNING,
    COMPLETED,
    CANCELLED,
    ERROR
}

/**
 * Live progress snapshot emitted during a run.
 */
data class RunProgress(
    val stats: RunStats,
    val currentCandidateIndex: Int,
    val currentRecipeName: String,
    val lastDetectorSummary: String,
    val latestEvents: List<CandidateResult>
)
