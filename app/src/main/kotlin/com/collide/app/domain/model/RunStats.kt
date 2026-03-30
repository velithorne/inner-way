package com.collide.app.domain.model

data class RunStats(
    val candidatesSeen: Int = 0,
    val candidatesPrunedPreEval: Int = 0,
    val candidatesEvaluated: Int = 0,
    val exactnessFailures: Int = 0,
    val hashMismatches: Int = 0,
    val notApplicableCount: Int = 0,
    val noGainCount: Int = 0,
    val strictWinnerCount: Int = 0,
    val encodeErrors: Int = 0,
    val decodeErrors: Int = 0,
    val metadataAccountingFailures: Int = 0,
    val elapsedMs: Long = 0L
) {
    fun withResult(result: CandidateResult): RunStats = when (result.classification) {
        CandidateClassification.STRICT_WINNER -> copy(
            candidatesEvaluated = candidatesEvaluated + 1,
            strictWinnerCount = strictWinnerCount + 1
        )
        CandidateClassification.NO_STRICT_IMPROVEMENT -> copy(
            candidatesEvaluated = candidatesEvaluated + 1,
            noGainCount = noGainCount + 1
        )
        CandidateClassification.EXACTNESS_FAILED -> copy(
            candidatesEvaluated = candidatesEvaluated + 1,
            exactnessFailures = exactnessFailures + 1
        )
        CandidateClassification.HASH_MISMATCH -> copy(
            candidatesEvaluated = candidatesEvaluated + 1,
            hashMismatches = hashMismatches + 1
        )
        CandidateClassification.NOT_APPLICABLE -> copy(
            candidatesEvaluated = candidatesEvaluated + 1,
            notApplicableCount = notApplicableCount + 1
        )
        CandidateClassification.PRUNED_PRE_EVAL -> copy(
            candidatesPrunedPreEval = candidatesPrunedPreEval + 1
        )
        CandidateClassification.ENCODE_ERROR -> copy(
            candidatesEvaluated = candidatesEvaluated + 1,
            encodeErrors = encodeErrors + 1
        )
        CandidateClassification.DECODE_ERROR -> copy(
            candidatesEvaluated = candidatesEvaluated + 1,
            decodeErrors = decodeErrors + 1
        )
        CandidateClassification.METADATA_ACCOUNTING_FAILURE -> copy(
            candidatesEvaluated = candidatesEvaluated + 1,
            metadataAccountingFailures = metadataAccountingFailures + 1
        )
        CandidateClassification.REPLAY_MISMATCH -> copy(
            candidatesEvaluated = candidatesEvaluated + 1
        )
    }

    /** All classifications accounted-for (used for cross-check assertions in tests). */
    val totalAccountedFor: Int
        get() = candidatesPrunedPreEval + candidatesEvaluated
}
