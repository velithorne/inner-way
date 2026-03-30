package com.collide.app.domain.model

/**
 * The output of applying a collision recipe to one or two normalized inputs.
 * Classification is honest — we track all outcomes including failures and non-events.
 */
data class CandidateResult(
    val index: Int,
    val recipeId: String,
    val recipeName: String,
    val classification: CandidateClassification,
    val candidateText: String,
    val candidateTokenCount: Int,
    val detectorResults: List<DetectorResult>,
    val evaluationNotes: String,
    val replayable: Boolean,
    val elapsedMs: Long
) {
    fun isEvent(): Boolean = classification == CandidateClassification.SAVED_EVENT
    fun bestScore(): Float = detectorResults.maxOfOrNull { it.score } ?: 0f
    fun passedDetectors(): List<DetectorResult> = detectorResults.filter { it.passed }
}

enum class CandidateClassification {
    SAVED_EVENT,
    NOT_INTERESTING_ENOUGH,
    INVALID_STRUCTURE,
    PARSE_FAILED,
    PRUNED_PRE_EVAL,
    UNSUPPORTED_FOR_BEHAVIOR_CHECK,
    DETECTOR_THRESHOLD_NOT_MET,
    EVALUATION_ERROR;

    fun displayName(): String = when (this) {
        SAVED_EVENT -> "Saved Event"
        NOT_INTERESTING_ENOUGH -> "Not Interesting"
        INVALID_STRUCTURE -> "Invalid Structure"
        PARSE_FAILED -> "Parse Failed"
        PRUNED_PRE_EVAL -> "Pruned"
        UNSUPPORTED_FOR_BEHAVIOR_CHECK -> "Unsupported Check"
        DETECTOR_THRESHOLD_NOT_MET -> "Below Threshold"
        EVALUATION_ERROR -> "Evaluation Error"
    }
}

/**
 * Result from a single detector applied to a candidate.
 */
data class DetectorResult(
    val detectorId: String,
    val detectorName: String,
    val score: Float,           // 0.0..1.0
    val threshold: Float,
    val passed: Boolean,
    val reason: String,
    val eventType: EventType?   // non-null if this detector suggests an event type
)

enum class EventType(val displayName: String) {
    STRUCTURAL_SIMPLIFICATION("Structural Simplification"),
    HYBRID_STRUCTURE_FOUND("Hybrid Structure Found"),
    REUSABLE_SCAFFOLD_EXTRACTED("Reusable Scaffold Extracted"),
    SYMMETRY_HINT("Symmetry Hint"),
    CONTRADICTION_DETECTED("Contradiction Detected"),
    UNEXPECTED_MERGE("Unexpected Merge"),
    COHERENT_VARIANT("Coherent Variant")
}
