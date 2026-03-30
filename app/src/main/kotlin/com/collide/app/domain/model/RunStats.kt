package com.collide.app.domain.model

data class RunStats(
    val candidatesSeen: Int = 0,
    val candidatesPrunedPreEval: Int = 0,
    val candidatesEvaluated: Int = 0,
    val exactnessFailures: Int = 0,
    val notApplicableCount: Int = 0,
    val noGainCount: Int = 0,
    val strictWinnerCount: Int = 0,
    val elapsedMs: Long = 0L
) {
    fun withResult(result: CandidateResult): RunStats {
        return when (result.classification) {
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
            CandidateClassification.NOT_APPLICABLE -> copy(
                candidatesEvaluated = candidatesEvaluated + 1,
                notApplicableCount = notApplicableCount + 1
            )
            CandidateClassification.PRUNED_PRE_EVAL -> copy(
                candidatesPrunedPreEval = candidatesPrunedPreEval + 1
            )
            CandidateClassification.ENCODE_ERROR, CandidateClassification.DECODE_ERROR -> copy(
                candidatesEvaluated = candidatesEvaluated + 1,
                exactnessFailures = exactnessFailures + 1
            )
        }
    }
}
