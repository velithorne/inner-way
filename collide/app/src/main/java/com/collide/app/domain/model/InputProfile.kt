package com.collide.app.domain.model

/**
 * Heuristic language/profile detection result.
 * This is an estimate only — it does not claim full parser certainty.
 */
data class InputProfile(
    val estimatedLanguage: LanguageHint,
    val confidence: Float,           // 0.0..1.0, lower = less certain
    val tokenCountEstimate: Int,
    val lineCount: Int,
    val averageLineLength: Float,
    val bracketDepthMax: Int,
    val keywordsDetected: List<String>,
    val parseStatus: ParseStatus = ParseStatus.NOT_ATTEMPTED,
    val normalizationStatus: NormalizationStatus = NormalizationStatus.NOT_ATTEMPTED
)

enum class LanguageHint {
    PLAIN_TEXT,
    PSEUDO_CODE,
    PYTHON_LIKE,
    JAVASCRIPT_LIKE,
    KOTLIN_LIKE,
    C_LIKE,
    JSON_LIKE,
    RULE_SNIPPET,
    UNKNOWN;

    fun displayName(): String = when (this) {
        PLAIN_TEXT -> "Plain Text"
        PSEUDO_CODE -> "Pseudo-code"
        PYTHON_LIKE -> "Python-like"
        JAVASCRIPT_LIKE -> "JavaScript-like"
        KOTLIN_LIKE -> "Kotlin-like"
        C_LIKE -> "C-like"
        JSON_LIKE -> "JSON-like"
        RULE_SNIPPET -> "Rule Snippet"
        UNKNOWN -> "Unknown"
    }
}

enum class ParseStatus {
    NOT_ATTEMPTED,
    PARTIAL,
    SUCCESS,
    FAILED
}

enum class NormalizationStatus {
    NOT_ATTEMPTED,
    SUCCESS,
    PARTIAL,
    FAILED
}
