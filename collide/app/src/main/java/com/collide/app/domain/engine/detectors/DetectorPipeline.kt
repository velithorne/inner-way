package com.collide.app.domain.engine.detectors

import com.collide.app.domain.model.CandidateClassification
import com.collide.app.domain.model.CandidateResult
import com.collide.app.domain.model.ColliderConfig
import com.collide.app.domain.model.DetectorResult
import com.collide.app.domain.model.EventType
import com.collide.app.domain.model.RunMode

/**
 * Orchestrates all detectors against a candidate.
 * Determines the final CandidateClassification based on detector results.
 *
 * Detector selection depends on run mode:
 * - Safe: only cheap detectors (novelty, compression)
 * - Balanced/Burst: all detectors
 */
class DetectorPipeline(
    private val config: ColliderConfig
) {
    private val noveltyDetector = StructuralNoveltyDetector()
    private val compressionDetector = StructuralCompressionDetector()
    private val patternDetector = ReusablePatternDetector()
    private val symmetryDetector = SymmetryHintDetector()
    private val hybridizationDetector = HybridizationDetector()
    private val contradictionDetector = ContradictionDetector()

    private val thresholdMultiplier = config.detectorSensitivity.thresholdMultiplier

    fun evaluate(
        candidate: CandidateResult,
        originalTextA: String,
        originalTextB: String?
    ): CandidateResult {
        if (candidate.classification == CandidateClassification.PARSE_FAILED ||
            candidate.classification == CandidateClassification.PRUNED_PRE_EVAL) {
            return candidate
        }

        if (candidate.candidateText.isBlank()) {
            return candidate.copy(
                classification = CandidateClassification.INVALID_STRUCTURE,
                evaluationNotes = candidate.evaluationNotes + "; empty output"
            )
        }

        val detectorResults = runDetectors(candidate.candidateText, originalTextA, originalTextB)
        val classification = classify(detectorResults, candidate)
        val dominated = dominantEventType(detectorResults)

        return candidate.copy(
            classification = classification,
            detectorResults = detectorResults,
            evaluationNotes = buildEvalNotes(detectorResults, candidate.evaluationNotes)
        )
    }

    private fun runDetectors(
        candidateText: String,
        originalA: String,
        originalB: String?
    ): List<DetectorResult> {
        val detectors: List<BaseDetector> = if (config.runMode.runExpensiveDetectors) {
            listOf(
                noveltyDetector,
                compressionDetector,
                patternDetector,
                symmetryDetector,
                hybridizationDetector,
                contradictionDetector
            )
        } else {
            // Safe mode: only cheap detectors
            listOf(noveltyDetector, compressionDetector)
        }

        return detectors.map { detector ->
            detector.detect(candidateText, originalA, originalB, thresholdMultiplier)
        }
    }

    private fun classify(
        results: List<DetectorResult>,
        candidate: CandidateResult
    ): CandidateClassification {
        val passedAny = results.any { it.passed }
        val bestScore = results.maxOfOrNull { it.score } ?: 0f

        return when {
            bestScore < 0.01f -> CandidateClassification.NOT_INTERESTING_ENOUGH
            passedAny -> CandidateClassification.SAVED_EVENT
            bestScore > 0f -> CandidateClassification.DETECTOR_THRESHOLD_NOT_MET
            else -> CandidateClassification.NOT_INTERESTING_ENOUGH
        }
    }

    private fun dominantEventType(results: List<DetectorResult>): EventType? {
        return results.filter { it.passed && it.eventType != null }
            .maxByOrNull { it.score }
            ?.eventType
    }

    private fun buildEvalNotes(results: List<DetectorResult>, existing: String): String {
        val summaries = results.map { r ->
            "${r.detectorName}: ${if (r.passed) "PASS" else "fail"} (${r.score.toInt() * 100}%)"
        }
        return if (existing.isBlank()) summaries.joinToString(", ")
        else "$existing | ${summaries.joinToString(", ")}"
    }

    fun summarizeResults(results: List<DetectorResult>): String {
        return results.joinToString(" | ") { r ->
            "${r.detectorName}=${pct(r.score)} [${if (r.passed) "PASS" else "fail"}]"
        }
    }

    private fun pct(f: Float): String = "%.0f".format(f * 100) + "%"
}
