package com.collide.app.domain.evaluator

import com.collide.app.domain.detector.EventRecorder
import com.collide.app.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class EventRecorderTest {
    private val recorder = EventRecorder()

    private val baseline = BaselineResult(
        strategy = BaselineStrategy.RAW_DEFLATE,
        encodedSize = 1000L,
        elapsedMs = 5L
    )

    private val input = InputSample("test.txt", null, ByteArray(5000))

    private fun makeResult(
        classification: CandidateClassification,
        encodedSize: Long = 800L,
        baselineSize: Long = 1000L,
        reconstructionPassed: Boolean = true
    ) = CandidateResult(
        recipeSpec = RecipeSpec("r1", listOf("delta8"), BackendCompressor.DEFLATE_DEFAULT),
        classification = classification,
        encodedSize = encodedSize,
        baselineSize = baselineSize,
        byteSavings = baselineSize - encodedSize,
        reconstructionPassed = reconstructionPassed,
        elapsedMs = 10L
    )

    @Test
    fun `strict winner with verification creates event`() {
        val result = makeResult(CandidateClassification.STRICT_WINNER, encodedSize = 800L)
        val event = recorder.maybeCreateEvent(result, input, baseline)
        assertNotNull(event)
        assertEquals("test.txt", event!!.inputFileName)
        assertEquals(800L, event.winningSize)
        assertEquals(200L, event.byteSavings)
        assertTrue(event.verificationPassed)
    }

    @Test
    fun `no improvement does not create event`() {
        val result = makeResult(CandidateClassification.NO_STRICT_IMPROVEMENT, encodedSize = 1000L)
        val event = recorder.maybeCreateEvent(result, input, baseline)
        assertNull(event)
    }

    @Test
    fun `exactness failure does not create event`() {
        val result = makeResult(
            CandidateClassification.EXACTNESS_FAILED,
            encodedSize = 800L,
            reconstructionPassed = false
        )
        val event = recorder.maybeCreateEvent(result, input, baseline)
        assertNull(event)
    }

    @Test
    fun `winner with failed reconstruction does not create event`() {
        val result = makeResult(
            CandidateClassification.STRICT_WINNER,
            encodedSize = 800L,
            reconstructionPassed = false
        )
        val event = recorder.maybeCreateEvent(result, input, baseline)
        assertNull(event)
    }

    @Test
    fun `event has correct recipe summary`() {
        val result = makeResult(CandidateClassification.STRICT_WINNER)
        val event = recorder.maybeCreateEvent(result, input, baseline)
        assertNotNull(event)
        assertTrue(event!!.recipeSummary.contains("delta8"))
    }
}
