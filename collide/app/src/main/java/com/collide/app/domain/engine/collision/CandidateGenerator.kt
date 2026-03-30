package com.collide.app.domain.engine.collision

import com.collide.app.domain.model.CandidateClassification
import com.collide.app.domain.model.CandidateResult
import com.collide.app.domain.model.ColliderConfig
import com.collide.app.domain.model.CollisionMode
import com.collide.app.domain.model.DetectorResult
import com.collide.app.domain.model.NormalizedCodeModel
import com.collide.app.domain.model.RecipeOperation
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

/**
 * Generates collision candidates deterministically from normalized inputs.
 *
 * Phase 1 rules:
 * - bounded search only
 * - no hidden randomness (seed is explicit)
 * - stable ordering by recipe id + operation chain
 * - allow pre-eval pruning
 * - supports one-input mutation and two-input recombination
 */
class CandidateGenerator(
    private val transformationEngine: TransformationEngine
) {

    /**
     * Generate all candidates for a given config and inputs.
     * Yields candidates one at a time via a callback to support live progress.
     *
     * @param onCandidate called for each candidate as it is generated
     * @return list of all candidates (may be empty if cancelled)
     */
    suspend fun generateCandidates(
        modelA: NormalizedCodeModel,
        modelB: NormalizedCodeModel?,
        config: ColliderConfig,
        onCandidate: suspend (CandidateResult) -> Unit
    ) {
        val mode = if (modelB != null && config.twoInputMode)
            CollisionMode.DUAL_INPUT_RECOMBINATION
        else
            CollisionMode.SINGLE_INPUT_MUTATION

        val recipes = RecipeLibrary.recipesFor(mode)
        var candidateIndex = 0

        outer@ for (recipe in recipes) {
            if (candidateIndex >= config.maxCandidates) break

            // Depth controls how many recipe variations to chain
            val depth = config.searchDepth.coerceIn(1, recipe.operations.size)

            // Generate sub-chain candidates: try truncated operation chains
            for (chainLen in 1..depth) {
                if (!coroutineContext.isActive) break@outer
                if (candidateIndex >= config.maxCandidates) break@outer

                val subRecipe = recipe.copy(
                    id = "${recipe.id}_d$chainLen",
                    operations = recipe.operations.take(chainLen)
                )

                // Pre-eval pruning for aggressive mode
                if (config.runMode.pruningAggressive && chainLen == 1 &&
                    shouldPruneEarly(subRecipe, modelA)) {
                    val pruned = CandidateResult(
                        index = candidateIndex++,
                        recipeId = subRecipe.id,
                        recipeName = subRecipe.name,
                        classification = CandidateClassification.PRUNED_PRE_EVAL,
                        candidateText = "",
                        candidateTokenCount = 0,
                        detectorResults = emptyList(),
                        evaluationNotes = "Pruned before evaluation (aggressive mode)",
                        replayable = false,
                        elapsedMs = 0L
                    )
                    onCandidate(pruned)
                    continue
                }

                val startMs = System.currentTimeMillis()
                val result = transformationEngine.applyRecipe(subRecipe, modelA, modelB)
                val elapsedMs = System.currentTimeMillis() - startMs

                val candidate = when (result) {
                    is TransformResult.Success -> CandidateResult(
                        index = candidateIndex++,
                        recipeId = subRecipe.id,
                        recipeName = subRecipe.name,
                        classification = CandidateClassification.NOT_INTERESTING_ENOUGH, // updated by evaluator
                        candidateText = result.text,
                        candidateTokenCount = result.text.split(Regex("\\s+")).size,
                        detectorResults = emptyList(),
                        evaluationNotes = result.notes,
                        replayable = true,
                        elapsedMs = elapsedMs
                    )
                    is TransformResult.Partial -> CandidateResult(
                        index = candidateIndex++,
                        recipeId = subRecipe.id,
                        recipeName = subRecipe.name,
                        classification = CandidateClassification.NOT_INTERESTING_ENOUGH,
                        candidateText = result.text,
                        candidateTokenCount = result.text.split(Regex("\\s+")).size,
                        detectorResults = emptyList(),
                        evaluationNotes = "Partial: ${result.reason}",
                        replayable = true,
                        elapsedMs = elapsedMs
                    )
                    is TransformResult.Failure -> CandidateResult(
                        index = candidateIndex++,
                        recipeId = subRecipe.id,
                        recipeName = subRecipe.name,
                        classification = CandidateClassification.PARSE_FAILED,
                        candidateText = "",
                        candidateTokenCount = 0,
                        detectorResults = emptyList(),
                        evaluationNotes = "Failure: ${result.reason}",
                        replayable = false,
                        elapsedMs = elapsedMs
                    )
                }

                onCandidate(candidate)
            }
        }
    }

    private fun shouldPruneEarly(
        recipe: com.collide.app.domain.model.CollisionRecipe,
        modelA: NormalizedCodeModel
    ): Boolean {
        // Prune if only one operation and input is very small — not worth evaluating
        return recipe.operations.size == 1 &&
               modelA.tokenCount < 5 &&
               recipe.operations[0].operationId in listOf(
                   RecipeOperation.REORDER_SAFE_BLOCKS,
                   RecipeOperation.EXTRACT_COMMON_SCAFFOLD
               )
    }
}
