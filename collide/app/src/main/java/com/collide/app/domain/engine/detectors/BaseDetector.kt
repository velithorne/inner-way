package com.collide.app.domain.engine.detectors

import com.collide.app.domain.model.DetectorResult
import com.collide.app.domain.model.EventType

/**
 * Base interface for all Phase 1 detectors.
 * Each detector returns a DetectorResult with a score, reason, and threshold pass/fail.
 * Detectors do NOT claim semantic correctness — they measure structural properties only.
 */
interface BaseDetector {
    val detectorId: String
    val detectorName: String
    val defaultThreshold: Float

    fun detect(
        candidateText: String,
        originalText: String,
        candidateB: String? = null,
        thresholdMultiplier: Float = 1.0f
    ): DetectorResult
}
