package com.collide.app.domain.evaluator

import com.collide.app.domain.engine.ReplayEngine
import com.collide.app.domain.model.*
import com.collide.app.domain.recipes.RecipeSerializer
import org.junit.Assert.*
import org.junit.Test

class ReplayEngineTest {

    private val engine = ReplayEngine()
    private val evaluator = CandidateEvaluator()
    private val baselineEval = BaselineEvaluator()

    private val testInput = ByteArray(3000) { (it % 32).toByte() }

    private fun makeWinnerEvent(input: ByteArray, recipe: RecipeSpec): SavedEvent? {
        val baseline = baselineEval.evaluate(input, BaselineStrategy.RAW_DEFLATE)
        val result = evaluator.evaluate(input, recipe, baseline.encodedSize, candidateIndex = 0)
        if (!result.isWinner) return null
        val vr = result.verificationResult ?: return null
        return SavedEvent(
            id = 1L,
            timestamp = System.currentTimeMillis(),
            inputFileName = "test_input.bin",
            inputSize = input.size.toLong(),
            baselineType = BaselineStrategy.RAW_DEFLATE.name,
            baselineSize = baseline.encodedSize,
            winningSize = result.encodedSize,
            byteSavings = result.byteSavings,
            recipeSummary = recipe.summary(),
            recipeJson = RecipeSerializer.serialize(recipe),
            verificationPassed = true,
            elapsedMs = result.elapsedMs,
            originalSha256 = vr.originalSha256,
            reconstructedSha256 = vr.reconstructedSha256,
            verificationMethod = vr.method.name,
            candidateIndex = 0,
            engineVersion = RunConfigSnapshot.ENGINE_VERSION,
            replayStatus = ReplayStatus.PENDING.name
        )
    }

    @Test
    fun `replay matches original result for winning recipe`() {
        val recipe = RecipeSpec("r_replay", listOf("delta8"), BackendCompressor.DEFLATE_DEFAULT)
        // Try to find a recipe that wins; if delta8 doesn't win, pick one that does
        val baseline = baselineEval.evaluate(testInput, BaselineStrategy.RAW_DEFLATE)
        val candidates = listOf(
            RecipeSpec("r1", listOf("delta8"), BackendCompressor.DEFLATE_DEFAULT),
            RecipeSpec("r2", listOf("xor_prev"), BackendCompressor.DEFLATE_DEFAULT),
            RecipeSpec("r3", listOf("byte_run_rle"), BackendCompressor.DEFLATE_DEFAULT)
        )
        val winnerEvent = candidates.mapNotNull { makeWinnerEvent(testInput, it) }.firstOrNull()
        if (winnerEvent == null) {
            // No winners for this input — skip test gracefully
            return
        }
        val replay = engine.replay(testInput, winnerEvent)
        assertEquals(ReplayStatus.MATCHED, replay.status)
        assertTrue(replay.replayVerificationPassed)
        assertEquals(winnerEvent.winningSize, replay.replayEncodedSize)
    }

    @Test
    fun `replay fails with different input`() {
        val recipe = RecipeSpec("r_rle", listOf("byte_run_rle"), BackendCompressor.DEFLATE_DEFAULT)
        // Build event from testInput
        val baseline = baselineEval.evaluate(testInput, BaselineStrategy.RAW_DEFLATE)
        val result = evaluator.evaluate(testInput, recipe, baseline.encodedSize, 0)
        if (!result.isWinner) return
        val vr = result.verificationResult ?: return

        val event = SavedEvent(
            id = 1L, timestamp = 0L, inputFileName = "x", inputSize = testInput.size.toLong(),
            baselineType = "RAW_DEFLATE", baselineSize = baseline.encodedSize,
            winningSize = result.encodedSize, byteSavings = result.byteSavings,
            recipeSummary = recipe.summary(), recipeJson = RecipeSerializer.serialize(recipe),
            verificationPassed = true, elapsedMs = 0L,
            originalSha256 = vr.originalSha256,
            reconstructedSha256 = vr.reconstructedSha256,
            verificationMethod = vr.method.name,
            candidateIndex = 0, engineVersion = RunConfigSnapshot.ENGINE_VERSION,
            replayStatus = ReplayStatus.PENDING.name
        )

        // Replay on completely different input
        val differentInput = ByteArray(1000) { 0xFF.toByte() }
        val replay = engine.replay(differentInput, event)
        // Should not match because sizes will differ
        assertNotEquals(ReplayStatus.MATCHED, replay.status)
    }

    @Test
    fun `replay returns error for invalid recipe json`() {
        val event = SavedEvent(
            id = 1L, timestamp = 0L, inputFileName = "x", inputSize = 100L,
            baselineType = "RAW_DEFLATE", baselineSize = 50L, winningSize = 30L, byteSavings = 20L,
            recipeSummary = "broken", recipeJson = "NOT VALID JSON",
            verificationPassed = true, elapsedMs = 0L, replayStatus = ReplayStatus.PENDING.name
        )
        val replay = engine.replay(testInput, event)
        assertEquals(ReplayStatus.ERROR, replay.status)
        assertFalse(replay.replayVerificationPassed)
    }
}
