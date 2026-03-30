package com.collide.app.domain.detector

import com.collide.app.domain.model.CandidateClassification
import com.collide.app.domain.model.CandidateResult
import com.collide.app.domain.model.InputSample
import com.collide.app.domain.model.SavedEvent
import com.collide.app.domain.model.BaselineResult
import com.collide.app.domain.recipes.RecipeSerializer

class EventRecorder {

    /**
     * Returns a SavedEvent if the result qualifies as a strict winner,
     * or null if it does not meet the criteria.
     *
     * Win criteria:
     * - classification == STRICT_WINNER
     * - reconstructionPassed == true
     * - encodedSize < baselineSize (strict)
     */
    fun maybeCreateEvent(
        result: CandidateResult,
        input: InputSample,
        baseline: BaselineResult
    ): SavedEvent? {
        if (result.classification != CandidateClassification.STRICT_WINNER) return null
        if (!result.reconstructionPassed) return null
        if (result.encodedSize >= result.baselineSize) return null

        return SavedEvent(
            id = 0, // assigned by Room
            timestamp = System.currentTimeMillis(),
            inputFileName = input.fileName,
            inputSize = input.size.toLong(),
            baselineType = baseline.strategy.name,
            baselineSize = baseline.encodedSize,
            winningSize = result.encodedSize,
            byteSavings = result.byteSavings,
            recipeSummary = result.recipeSpec.summary(),
            recipeJson = RecipeSerializer.serialize(result.recipeSpec),
            verificationPassed = true,
            elapsedMs = result.elapsedMs,
            notes = "Collision found: ${result.recipeSpec.transformIds.size}-step chain"
        )
    }
}
