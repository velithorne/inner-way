package com.collide.app.engine

import com.collide.app.domain.engine.CodeColliderEngine
import com.collide.app.domain.model.CandidateClassification
import com.collide.app.domain.model.ColliderConfig
import com.collide.app.domain.model.DetectorSensitivity
import com.collide.app.domain.model.InputSample
import com.collide.app.domain.model.RunMode
import com.collide.app.domain.model.RunStatus
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Integration test for the full Code Collider engine pipeline.
 * Validates the end-to-end loop: input → normalize → generate → detect → classify.
 */
class ColliderEngineIntegrationTest {

    private val engine = CodeColliderEngine()

    @Test
    fun `engine completes run on simple input`() = runTest {
        val input = InputSample("t1", "test", "def foo(a, b): return a + b")
        val config = ColliderConfig(runMode = RunMode.SAFE, maxCandidates = 10, searchDepth = 1)

        val progressUpdates = engine.runCollision(input, null, config).toList()

        assertTrue("should have progress updates", progressUpdates.isNotEmpty())
        val final = progressUpdates.last()
        assertEquals("run should complete", RunStatus.COMPLETED, final.stats.status)
    }

    @Test
    fun `candidates seen count is non-zero`() = runTest {
        val input = InputSample("t2", "test", """
            def process(items):
                result = []
                for item in items:
                    if item > 0:
                        result.append(item)
                return result
        """.trimIndent())
        val config = ColliderConfig(runMode = RunMode.SAFE, maxCandidates = 15, searchDepth = 2)

        val final = engine.runCollision(input, null, config).last()
        assertTrue("should have seen candidates", final.stats.candidatesSeen > 0)
    }

    @Test
    fun `honest accounting - total adds up`() = runTest {
        val input = InputSample("t3", "test", """
            function computeTotal(items) {
                let total = 0;
                for (let i = 0; i < items.length; i++) {
                    total += items[i].price;
                }
                return total;
            }
        """.trimIndent())
        val config = ColliderConfig(runMode = RunMode.BALANCED, maxCandidates = 30, searchDepth = 3)

        val final = engine.runCollision(input, null, config).last()
        val stats = final.stats

        // Verify accounting is consistent
        val accounted = stats.candidatesPrunedPreEval + stats.candidatesEvaluated + stats.parseFailures
        assertEquals(
            "pruned + evaluated + parse failures should equal seen",
            stats.candidatesSeen,
            accounted
        )
    }

    @Test
    fun `dual input mode runs without error`() = runTest {
        val inputA = InputSample("a1", "A",
            "def add(a, b): return a + b\ndef mul(a, b): return a * b")
        val inputB = InputSample("b1", "B",
            "def sum(x, y): return x + y\ndef product(x, y): return x * y")
        val config = ColliderConfig(
            runMode = RunMode.SAFE,
            maxCandidates = 15,
            searchDepth = 2,
            twoInputMode = true
        )

        val final = engine.runCollision(inputA, inputB, config).last()
        assertEquals("run should complete", RunStatus.COMPLETED, final.stats.status)
        assertTrue("should process candidates", final.stats.candidatesSeen > 0)
    }

    @Test
    fun `high sensitivity finds more events than low sensitivity`() = runTest {
        val input = InputSample("t5", "test", """
            def classify(score):
                if score >= 90:
                    return "A"
                elif score >= 80:
                    return "B"
                elif score >= 70:
                    return "C"
                else:
                    return "F"
        """.trimIndent())

        val highConfig = ColliderConfig(
            runMode = RunMode.BURST,
            maxCandidates = 50,
            searchDepth = 4,
            detectorSensitivity = DetectorSensitivity.HIGH
        )
        val lowConfig = ColliderConfig(
            runMode = RunMode.BURST,
            maxCandidates = 50,
            searchDepth = 4,
            detectorSensitivity = DetectorSensitivity.LOW
        )

        val highFinal = engine.runCollision(input, null, highConfig).last()
        val lowFinal = engine.runCollision(input, null, lowConfig).last()

        // High sensitivity should find at least as many events as low sensitivity
        assertTrue(
            "high sensitivity should find >= events than low sensitivity " +
            "(high=${highFinal.stats.savedEventCount}, low=${lowFinal.stats.savedEventCount})",
            highFinal.stats.savedEventCount >= lowFinal.stats.savedEventCount
        )
    }

    @Test
    fun `all final classifications are valid`() = runTest {
        val input = InputSample("t6", "test", "val x = if (a > b) a else b")
        val config = ColliderConfig(runMode = RunMode.BALANCED, maxCandidates = 20, searchDepth = 2)

        val allUpdates = engine.runCollision(input, null, config).toList()
        // Verify no unexpected classification states in final events
        allUpdates.last().latestEvents.forEach { event ->
            assertEquals(
                "events in latestEvents should all be SAVED_EVENT",
                CandidateClassification.SAVED_EVENT,
                event.classification
            )
        }
    }
}
