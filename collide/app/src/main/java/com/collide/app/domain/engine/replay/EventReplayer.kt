package com.collide.app.domain.engine.replay

import com.collide.app.domain.engine.collision.RecipeSerializer
import com.collide.app.domain.engine.collision.TransformationEngine
import com.collide.app.domain.engine.collision.TransformResult
import com.collide.app.domain.engine.detectors.DetectorPipeline
import com.collide.app.domain.engine.normalize.CodeNormalizer
import com.collide.app.domain.model.CandidateClassification
import com.collide.app.domain.model.CandidateResult
import com.collide.app.domain.model.ColliderConfig
import com.collide.app.domain.model.InputSample
import com.collide.app.domain.model.InputSourceType
import com.collide.app.domain.model.ReplayStatus
import com.collide.app.domain.model.SavedEvent

/**
 * Replays a saved event by:
 * 1. Rebuilding the normalized input(s)
 * 2. Re-running the exact collision recipe
 * 3. Regenerating the candidate
 * 4. Re-running the detectors
 * 5. Comparing results to the saved event
 */
class EventReplayer(
    private val normalizer: CodeNormalizer,
    private val transformationEngine: TransformationEngine,
    private val recipeSerializer: RecipeSerializer
) {

    data class ReplayResult(
        val status: ReplayStatus,
        val replayedText: String,
        val originalText: String,
        val notes: String,
        val detectorSummary: String
    )

    fun replay(
        event: SavedEvent,
        originalTextA: String,
        originalTextB: String?,
        config: ColliderConfig
    ): ReplayResult {
        if (!event.replayable) {
            return ReplayResult(
                ReplayStatus.REPLAY_FAILED,
                "", event.candidatePreview,
                "event was marked non-replayable",
                ""
            )
        }

        val recipe = recipeSerializer.deserialize(event.collisionRecipeJson)
            ?: return ReplayResult(
                ReplayStatus.REPLAY_FAILED, "", event.candidatePreview,
                "failed to deserialize recipe", ""
            )

        val sampleA = InputSample("replay_a", event.inputLabelA, originalTextA, InputSourceType.PASTE)
        val modelA = normalizer.normalize(sampleA)

        val modelB = originalTextB?.let { textB ->
            val sampleB = InputSample("replay_b", event.inputLabelB ?: "B", textB, InputSourceType.PASTE)
            normalizer.normalize(sampleB)
        }

        val replayResult = transformationEngine.applyRecipe(recipe, modelA, modelB)
        val replayedText = when (replayResult) {
            is TransformResult.Success -> replayResult.text
            is TransformResult.Partial -> replayResult.text
            is TransformResult.Failure -> return ReplayResult(
                ReplayStatus.REPLAY_FAILED, "", event.candidatePreview,
                "recipe re-run failed: ${replayResult.reason}", ""
            )
        }

        // Run detectors
        val pipeline = DetectorPipeline(config)
        val dummyCandidate = CandidateResult(
            index = 0,
            recipeId = recipe.id,
            recipeName = recipe.name,
            classification = CandidateClassification.NOT_INTERESTING_ENOUGH,
            candidateText = replayedText,
            candidateTokenCount = replayedText.split(Regex("\\s+")).size,
            detectorResults = emptyList(),
            evaluationNotes = "",
            replayable = true,
            elapsedMs = 0L
        )
        val evaluated = pipeline.evaluate(dummyCandidate, originalTextA, originalTextB)
        val detectorSummary = pipeline.summarizeResults(evaluated.detectorResults)

        // Compare: does the same event still appear?
        val savedPreviewNorm = normalizeForComparison(event.candidatePreview)
        val replayedNorm = normalizeForComparison(replayedText)

        val status = when {
            savedPreviewNorm == replayedNorm -> ReplayStatus.REPLAY_MATCHED
            evaluated.classification == CandidateClassification.SAVED_EVENT ->
                ReplayStatus.REPLAY_DIFFERED  // event still found but with different output
            else -> ReplayStatus.REPLAY_DIFFERED  // event still found but no longer interesting
        }

        val notes = buildString {
            append("Replay status: ${status.name}. ")
            if (status == ReplayStatus.REPLAY_MATCHED) {
                append("Output matches saved candidate.")
            } else {
                append("Output differs from saved candidate. ")
                append("Detector classification: ${evaluated.classification.displayName()}.")
            }
            if (replayResult is TransformResult.Partial) {
                append(" Warning: ${replayResult.reason}")
            }
        }

        return ReplayResult(status, replayedText, event.candidatePreview, notes, detectorSummary)
    }

    private fun normalizeForComparison(text: String): String =
        text.replace(Regex("\\s+"), " ").trim()
}
