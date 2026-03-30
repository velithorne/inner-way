package com.collide.app.domain.evaluator

import com.collide.app.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class RunStatsTest {

    private fun makeResult(classification: CandidateClassification) = CandidateResult(
        recipeSpec = RecipeSpec("r0", listOf("identity"), BackendCompressor.DEFLATE_DEFAULT),
        classification = classification,
        encodedSize = 100L,
        baselineSize = 120L,
        byteSavings = 20L,
        reconstructionPassed = classification == CandidateClassification.STRICT_WINNER,
        elapsedMs = 10L
    )

    @Test
    fun `strict winner increments winner count`() {
        val stats = RunStats().withResult(makeResult(CandidateClassification.STRICT_WINNER))
        assertEquals(1, stats.strictWinnerCount)
        assertEquals(1, stats.candidatesEvaluated)
    }

    @Test
    fun `no improvement increments no gain count`() {
        val stats = RunStats().withResult(makeResult(CandidateClassification.NO_STRICT_IMPROVEMENT))
        assertEquals(1, stats.noGainCount)
        assertEquals(1, stats.candidatesEvaluated)
        assertEquals(0, stats.strictWinnerCount)
    }

    @Test
    fun `exactness failure increments failure count`() {
        val stats = RunStats().withResult(makeResult(CandidateClassification.EXACTNESS_FAILED))
        assertEquals(1, stats.exactnessFailures)
        assertEquals(1, stats.candidatesEvaluated)
    }

    @Test
    fun `not applicable increments not applicable count`() {
        val stats = RunStats().withResult(makeResult(CandidateClassification.NOT_APPLICABLE))
        assertEquals(1, stats.notApplicableCount)
        assertEquals(1, stats.candidatesEvaluated)
    }

    @Test
    fun `pruned increments pruned count not evaluated`() {
        val stats = RunStats().withResult(makeResult(CandidateClassification.PRUNED_PRE_EVAL))
        assertEquals(1, stats.candidatesPrunedPreEval)
        assertEquals(0, stats.candidatesEvaluated)
    }

    @Test
    fun `multiple results accumulate correctly`() {
        var stats = RunStats(candidatesSeen = 10)
        stats = stats.withResult(makeResult(CandidateClassification.STRICT_WINNER))
        stats = stats.withResult(makeResult(CandidateClassification.STRICT_WINNER))
        stats = stats.withResult(makeResult(CandidateClassification.NO_STRICT_IMPROVEMENT))
        stats = stats.withResult(makeResult(CandidateClassification.EXACTNESS_FAILED))
        stats = stats.withResult(makeResult(CandidateClassification.NOT_APPLICABLE))

        assertEquals(10, stats.candidatesSeen)
        assertEquals(5, stats.candidatesEvaluated)
        assertEquals(2, stats.strictWinnerCount)
        assertEquals(1, stats.noGainCount)
        assertEquals(1, stats.exactnessFailures)
        assertEquals(1, stats.notApplicableCount)
    }
}
