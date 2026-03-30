package com.collide.app.domain.engine

import com.collide.app.domain.evaluator.CandidateEvaluator
import com.collide.app.domain.evaluator.ExactnessVerifier
import com.collide.app.domain.model.*
import com.collide.app.domain.recipes.RecipeSerializer

/**
 * Re-runs a saved recipe on the provided input and verifies the result matches
 * the archived event exactly.
 *
 * A replay is MATCHED if:
 * - Total encoded size equals the archived winningSize
 * - Exactness + SHA-256 verification passes
 * - The original SHA-256 of the re-run matches the archived originalSha256
 */
class ReplayEngine(
    private val evaluator: CandidateEvaluator = CandidateEvaluator()
) {

    fun replay(
        input: ByteArray,
        savedEvent: SavedEvent
    ): ReplayResult {
        val start = System.currentTimeMillis()
        return try {
            val recipe = RecipeSerializer.deserialize(savedEvent.recipeJson)
            val result = evaluator.evaluate(
                input = input,
                recipe = recipe,
                baselineSize = savedEvent.baselineSize,
                candidateIndex = savedEvent.candidateIndex
            )
            val elapsed = System.currentTimeMillis() - start
            val vr = result.verificationResult

            if (!result.reconstructionPassed || vr?.passed != true) {
                return ReplayResult(
                    status = ReplayStatus.VERIFY_FAILED,
                    replayEncodedSize = result.encodedSize,
                    replayVerificationPassed = false,
                    replayElapsedMs = elapsed,
                    replayOriginalSha256 = vr?.originalSha256 ?: "",
                    replayReconstructedSha256 = vr?.reconstructedSha256 ?: "",
                    errorMessage = vr?.failureReason ?: "Verification failed"
                )
            }

            val sizesMatch = result.encodedSize == savedEvent.winningSize
            val sha256Consistent = savedEvent.originalSha256.isEmpty() ||
                                   vr.originalSha256 == savedEvent.originalSha256

            val status = when {
                !sizesMatch -> ReplayStatus.SIZE_MISMATCH
                !sha256Consistent -> ReplayStatus.SIZE_MISMATCH
                else -> ReplayStatus.MATCHED
            }

            ReplayResult(
                status = status,
                replayEncodedSize = result.encodedSize,
                replayVerificationPassed = true,
                replayElapsedMs = elapsed,
                replayOriginalSha256 = vr.originalSha256,
                replayReconstructedSha256 = vr.reconstructedSha256,
                errorMessage = if (status != ReplayStatus.MATCHED)
                    "Archived size=${savedEvent.winningSize}, replayed=${result.encodedSize}" else null
            )
        } catch (e: Exception) {
            ReplayResult(
                status = ReplayStatus.ERROR,
                replayEncodedSize = 0L,
                replayVerificationPassed = false,
                replayElapsedMs = System.currentTimeMillis() - start,
                errorMessage = e.message ?: "Unknown replay error"
            )
        }
    }
}
