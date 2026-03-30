package com.collide.app.domain.model

/**
 * Configuration for a single collision run, derived from user settings and UI selections.
 */
data class ColliderConfig(
    val runMode: RunMode = RunMode.BALANCED,
    val searchDepth: Int = RunMode.BALANCED.searchDepth,
    val maxCandidates: Int = RunMode.BALANCED.maxCandidates,
    val detectorSensitivity: DetectorSensitivity = DetectorSensitivity.MEDIUM,
    val twoInputMode: Boolean = false,
    val saveNearMisses: Boolean = false,
    val maxInputSizeBytes: Int = InputSample.MAX_INPUT_SIZE_BYTES
)

enum class DetectorSensitivity(val label: String, val thresholdMultiplier: Float) {
    LOW("Low", 1.4f),
    MEDIUM("Medium", 1.0f),
    HIGH("High", 0.6f)
}
