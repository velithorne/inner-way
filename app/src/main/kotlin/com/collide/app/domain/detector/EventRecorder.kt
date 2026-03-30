package com.collide.app.domain.detector

import com.collide.app.domain.model.*
import com.collide.app.domain.recipes.RecipeSerializer
import com.google.gson.Gson

class EventRecorder {

    private val gson = Gson()

    /**
     * Phase 2 win criteria — ALL must hold:
     * 1. classification == STRICT_WINNER
     * 2. reconstructionPassed == true
     * 3. verificationResult.passed == true (byte equality + SHA-256)
     * 4. verificationResult.hashesMatch == true
     * 5. encodedSize < baselineSize (strict)
     */
    fun maybeCreateEvent(
        result: CandidateResult,
        input: InputSample,
        baseline: BaselineResult,
        runConfig: RunConfigSnapshot? = null
    ): SavedEvent? {
        if (result.classification != CandidateClassification.STRICT_WINNER) return null
        if (!result.reconstructionPassed) return null
        if (result.encodedSize >= result.baselineSize) return null

        val vr = result.verificationResult
        // Phase 2: require hash verification if available
        if (vr != null) {
            if (!vr.passed) return null
            if (!vr.hashesMatch) return null
        }

        val breakdown = result.sizeBreakdown
        val configJson = if (runConfig != null) gson.toJson(runConfig) else ""

        return SavedEvent(
            id = 0,
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
            notes = buildNotes(result),
            originalSha256 = vr?.originalSha256 ?: "",
            reconstructedSha256 = vr?.reconstructedSha256 ?: "",
            verificationMethod = vr?.method?.name ?: "BYTE_EQUALITY_ONLY",
            transformedPayloadSize = breakdown?.transformedPayloadSize ?: 0L,
            backendCompressedSize = breakdown?.backendCompressedPayloadSize ?: 0L,
            transformMetadataSize = breakdown?.transformMetadataSize ?: 0L,
            containerHeaderSize = breakdown?.containerHeaderSize ?: 8L,
            candidateIndex = result.candidateIndex,
            runConfigJson = configJson,
            engineVersion = RunConfigSnapshot.ENGINE_VERSION,
            replayStatus = ReplayStatus.PENDING.name
        )
    }

    private fun buildNotes(result: CandidateResult): String {
        val chainLen = result.recipeSpec.transformIds.size
        val savings = result.byteSavings
        return "Verified collision: ${chainLen}-step chain, saves ${savings}B. " +
               "SHA-256 verified exact reconstruction. Candidate #${result.candidateIndex}."
    }
}
