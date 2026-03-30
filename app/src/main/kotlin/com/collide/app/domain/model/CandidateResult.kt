package com.collide.app.domain.model

enum class CandidateClassification {
    STRICT_WINNER,
    NO_STRICT_IMPROVEMENT,
    EXACTNESS_FAILED,
    HASH_MISMATCH,
    NOT_APPLICABLE,
    PRUNED_PRE_EVAL,
    ENCODE_ERROR,
    DECODE_ERROR,
    METADATA_ACCOUNTING_FAILURE,
    REPLAY_MISMATCH;

    val displayLabel: String get() = when (this) {
        STRICT_WINNER -> "Strict Winner"
        NO_STRICT_IMPROVEMENT -> "No Improvement"
        EXACTNESS_FAILED -> "Exactness Failed"
        HASH_MISMATCH -> "Hash Mismatch"
        NOT_APPLICABLE -> "Not Applicable"
        PRUNED_PRE_EVAL -> "Pruned"
        ENCODE_ERROR -> "Encode Error"
        DECODE_ERROR -> "Decode Error"
        METADATA_ACCOUNTING_FAILURE -> "Metadata Accounting Failure"
        REPLAY_MISMATCH -> "Replay Mismatch"
    }
}

data class CandidateResult(
    val recipeSpec: RecipeSpec,
    val classification: CandidateClassification,
    val encodedSize: Long,
    val baselineSize: Long,
    val byteSavings: Long,
    val reconstructionPassed: Boolean,
    val elapsedMs: Long,
    val errorMessage: String? = null,
    val reason: String? = null,
    val sizeBreakdown: SizeBreakdown? = null,
    val verificationResult: VerificationResult? = null,
    val candidateIndex: Int = -1
) {
    val isWinner: Boolean get() = classification == CandidateClassification.STRICT_WINNER
}
