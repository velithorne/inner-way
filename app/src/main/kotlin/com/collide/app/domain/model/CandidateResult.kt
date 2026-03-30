package com.collide.app.domain.model

enum class CandidateClassification {
    STRICT_WINNER,
    NO_STRICT_IMPROVEMENT,
    EXACTNESS_FAILED,
    NOT_APPLICABLE,
    PRUNED_PRE_EVAL,
    ENCODE_ERROR,
    DECODE_ERROR
}

data class CandidateResult(
    val recipeSpec: RecipeSpec,
    val classification: CandidateClassification,
    val encodedSize: Long,
    val baselineSize: Long,
    val byteSavings: Long,
    val reconstructionPassed: Boolean,
    val elapsedMs: Long,
    val errorMessage: String? = null
) {
    val isWinner: Boolean get() = classification == CandidateClassification.STRICT_WINNER
}
