package com.collide.app.domain.engine

import com.collide.app.domain.engine.collision.CandidateGenerator
import com.collide.app.domain.engine.collision.TransformationEngine
import com.collide.app.domain.engine.detectors.DetectorPipeline
import com.collide.app.domain.engine.evaluation.CandidateEvaluator
import com.collide.app.domain.engine.evaluation.ValidationResult
import com.collide.app.domain.engine.normalize.CodeNormalizer
import com.collide.app.domain.engine.normalize.InputProfiler
import com.collide.app.domain.model.CandidateClassification
import com.collide.app.domain.model.CandidateResult
import com.collide.app.domain.model.ColliderConfig
import com.collide.app.domain.model.InputProfile
import com.collide.app.domain.model.InputSample
import com.collide.app.domain.model.NormalizedCodeModel
import com.collide.app.domain.model.RunProgress
import com.collide.app.domain.model.RunStats
import com.collide.app.domain.model.RunStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * The top-level Code Collider engine.
 * Coordinates: profiling → normalization → candidate generation → detection → classification.
 *
 * This is the core loop of Phase 1:
 * 1. Profile inputs
 * 2. Normalize inputs
 * 3. Generate bounded collision candidates
 * 4. Evaluate with detectors
 * 5. Classify and record
 *
 * Emits RunProgress events via Flow for live UI updates.
 */
class CodeColliderEngine(
    private val profiler: InputProfiler = InputProfiler(),
    private val normalizer: CodeNormalizer = CodeNormalizer(),
    private val transformationEngine: TransformationEngine = TransformationEngine(),
    private val evaluator: CandidateEvaluator = CandidateEvaluator()
) {

    fun profileInput(sample: InputSample): InputProfile = profiler.profile(sample)

    fun normalizeInput(sample: InputSample): NormalizedCodeModel = normalizer.normalize(sample)

    /**
     * Run the full collision pipeline, emitting progress updates.
     * The flow completes when all candidates are processed or cancelled.
     */
    fun runCollision(
        inputA: InputSample,
        inputB: InputSample?,
        config: ColliderConfig
    ): Flow<RunProgress> = flow {
        val startTime = System.currentTimeMillis()
        var stats = RunStats(status = RunStatus.RUNNING)
        val events = mutableListOf<CandidateResult>()

        val modelA = normalizer.normalize(inputA)
        val modelB = inputB?.let { normalizer.normalize(it) }
        val pipeline = DetectorPipeline(config)
        val generator = CandidateGenerator(transformationEngine)

        emit(RunProgress(stats, 0, "Initializing...", "", emptyList()))

        generator.generateCandidates(modelA, modelB, config) { rawCandidate ->
            stats = stats.copy(candidatesSeen = stats.candidatesSeen + 1)

            val candidate = when (rawCandidate.classification) {
                CandidateClassification.PRUNED_PRE_EVAL -> {
                    stats = stats.copy(candidatesPrunedPreEval = stats.candidatesPrunedPreEval + 1)
                    rawCandidate
                }
                CandidateClassification.PARSE_FAILED -> {
                    stats = stats.copy(parseFailures = stats.parseFailures + 1)
                    rawCandidate
                }
                else -> {
                    // Validate structure first
                    val validated = when (val vr = evaluator.validateStructure(rawCandidate, modelA)) {
                        is ValidationResult.Invalid -> rawCandidate.copy(
                            classification = CandidateClassification.INVALID_STRUCTURE,
                            evaluationNotes = rawCandidate.evaluationNotes + "; " + vr.reason
                        )
                        is ValidationResult.Uninteresting -> rawCandidate.copy(
                            classification = CandidateClassification.NOT_INTERESTING_ENOUGH,
                            evaluationNotes = rawCandidate.evaluationNotes + "; " + vr.reason
                        )
                        is ValidationResult.Valid, is ValidationResult.Partial -> {
                            // Run detectors
                            val note = when (vr) {
                                is ValidationResult.Partial -> "; " + vr.reason
                                else -> ""
                            }
                            pipeline.evaluate(rawCandidate, inputA.rawText, inputB?.rawText)
                                .let { it.copy(evaluationNotes = it.evaluationNotes + note) }
                        }
                    }

                    stats = stats.copy(candidatesEvaluated = stats.candidatesEvaluated + 1)

                    when (validated.classification) {
                        CandidateClassification.INVALID_STRUCTURE ->
                            stats = stats.copy(invalidStructureCount = stats.invalidStructureCount + 1)
                        CandidateClassification.NOT_INTERESTING_ENOUGH,
                        CandidateClassification.DETECTOR_THRESHOLD_NOT_MET ->
                            stats = stats.copy(notInterestingCount = stats.notInterestingCount + 1)
                        CandidateClassification.SAVED_EVENT -> {
                            stats = stats.copy(savedEventCount = stats.savedEventCount + 1)
                            events.add(validated)
                        }
                        CandidateClassification.EVALUATION_ERROR ->
                            stats = stats.copy(evaluationErrors = (stats.candidatesEvaluated - stats.candidatesEvaluated))
                        else -> {}
                    }
                    validated
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            stats = stats.copy(elapsedMs = elapsed)

            val detectorSummary = if (candidate.detectorResults.isNotEmpty()) {
                pipeline.summarizeResults(candidate.detectorResults)
            } else candidate.classification.displayName()

            emit(RunProgress(
                stats = stats,
                currentCandidateIndex = candidate.index,
                currentRecipeName = candidate.recipeName,
                lastDetectorSummary = detectorSummary,
                latestEvents = events.toList()
            ))
        }

        val finalElapsed = System.currentTimeMillis() - startTime
        stats = stats.copy(
            elapsedMs = finalElapsed,
            status = RunStatus.COMPLETED
        )
        emit(RunProgress(stats, stats.candidatesSeen, "Complete", "Run finished", events.toList()))
    }
}
