package com.collide.app.domain.evaluator

import com.collide.app.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class RunStatsPhase2Test {

    private fun makeResult(cls: CandidateClassification) = CandidateResult(
        recipeSpec = RecipeSpec("r0", listOf("delta8"), BackendCompressor.DEFLATE_DEFAULT),
        classification = cls, encodedSize = 100L, baselineSize = 120L, byteSavings = 20L,
        reconstructionPassed = cls == CandidateClassification.STRICT_WINNER, elapsedMs = 1L
    )

    @Test
    fun `hash mismatch increments hashMismatches`() {
        val stats = RunStats().withResult(makeResult(CandidateClassification.HASH_MISMATCH))
        assertEquals(1, stats.hashMismatches)
        assertEquals(1, stats.candidatesEvaluated)
    }

    @Test
    fun `encode error increments encodeErrors`() {
        val stats = RunStats().withResult(makeResult(CandidateClassification.ENCODE_ERROR))
        assertEquals(1, stats.encodeErrors)
        assertEquals(1, stats.candidatesEvaluated)
    }

    @Test
    fun `decode error increments decodeErrors`() {
        val stats = RunStats().withResult(makeResult(CandidateClassification.DECODE_ERROR))
        assertEquals(1, stats.decodeErrors)
        assertEquals(1, stats.candidatesEvaluated)
    }

    @Test
    fun `metadata accounting failure increments its counter`() {
        val stats = RunStats().withResult(makeResult(CandidateClassification.METADATA_ACCOUNTING_FAILURE))
        assertEquals(1, stats.metadataAccountingFailures)
    }

    @Test
    fun `totalAccountedFor equals seen candidates`() {
        var stats = RunStats(candidatesSeen = 10)
        repeat(5) { stats = stats.withResult(makeResult(CandidateClassification.NO_STRICT_IMPROVEMENT)) }
        repeat(3) { stats = stats.withResult(makeResult(CandidateClassification.NOT_APPLICABLE)) }
        repeat(2) { stats = stats.withResult(makeResult(CandidateClassification.PRUNED_PRE_EVAL)) }
        assertEquals(10, stats.totalAccountedFor)
        assertEquals(10, stats.candidatesSeen)
    }
}
