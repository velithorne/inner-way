package com.collide.app.domain.model

data class SavedEvent(
    val id: Long,
    val timestamp: Long,
    val inputFileName: String,
    val inputSize: Long,
    val baselineType: String,
    val baselineSize: Long,
    val winningSize: Long,
    val byteSavings: Long,
    val recipeSummary: String,
    val recipeJson: String,
    val verificationPassed: Boolean,
    val elapsedMs: Long,
    val notes: String = ""
) {
    val compressionRatioPct: Double
        get() = if (baselineSize > 0) (byteSavings.toDouble() / baselineSize) * 100.0 else 0.0
}
