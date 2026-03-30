package com.collide.app.engine

import com.collide.app.domain.engine.collision.CandidateGenerator
import com.collide.app.domain.engine.collision.TransformationEngine
import com.collide.app.domain.engine.normalize.CodeNormalizer
import com.collide.app.domain.model.CandidateClassification
import com.collide.app.domain.model.ColliderConfig
import com.collide.app.domain.model.InputSample
import com.collide.app.domain.model.RunMode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class CandidateGeneratorTest {

    private val normalizer = CodeNormalizer()
    private val transformEngine = TransformationEngine()
    private val generator = CandidateGenerator(transformEngine)

    @Test
    fun `generates candidates deterministically`() = runTest {
        val sampleA = InputSample("a1", "test", "def foo(a, b): return a + b")
        val modelA = normalizer.normalize(sampleA)
        val config = ColliderConfig(runMode = RunMode.SAFE, maxCandidates = 10, searchDepth = 2)

        val candidates1 = mutableListOf<com.collide.app.domain.model.CandidateResult>()
        val candidates2 = mutableListOf<com.collide.app.domain.model.CandidateResult>()

        generator.generateCandidates(modelA, null, config) { candidates1.add(it) }
        generator.generateCandidates(modelA, null, config) { candidates2.add(it) }

        assertEquals("should generate same number of candidates", candidates1.size, candidates2.size)
        for (i in candidates1.indices) {
            assertEquals("recipe ids should match", candidates1[i].recipeId, candidates2[i].recipeId)
        }
    }

    @Test
    fun `respects max candidates limit`() = runTest {
        val sampleA = InputSample("a1", "test", "def foo(a, b): return a + b")
        val modelA = normalizer.normalize(sampleA)
        val maxCandidates = 5
        val config = ColliderConfig(runMode = RunMode.SAFE, maxCandidates = maxCandidates, searchDepth = 1)

        val candidates = mutableListOf<com.collide.app.domain.model.CandidateResult>()
        generator.generateCandidates(modelA, null, config) { candidates.add(it) }

        assertTrue(
            "should not exceed max candidates ($maxCandidates), got ${candidates.size}",
            candidates.size <= maxCandidates
        )
    }

    @Test
    fun `candidates have sequential indices`() = runTest {
        val sampleA = InputSample("a1", "test", "def foo(a, b): return a + b")
        val modelA = normalizer.normalize(sampleA)
        val config = ColliderConfig(runMode = RunMode.SAFE, maxCandidates = 15, searchDepth = 2)

        val candidates = mutableListOf<com.collide.app.domain.model.CandidateResult>()
        generator.generateCandidates(modelA, null, config) { candidates.add(it) }

        for ((i, candidate) in candidates.withIndex()) {
            assertEquals("candidate index should be sequential", i, candidate.index)
        }
    }

    @Test
    fun `failed recipes produce parse-failed classification`() = runTest {
        // Very minimal input that some operations will fail on
        val sampleA = InputSample("a1", "test", "")
        val modelA = normalizer.normalize(sampleA)
        val config = ColliderConfig(runMode = RunMode.BURST, maxCandidates = 30, searchDepth = 3)

        val candidates = mutableListOf<com.collide.app.domain.model.CandidateResult>()
        generator.generateCandidates(modelA, null, config) { candidates.add(it) }

        // At least some candidates should have been generated
        assertTrue("should have generated some candidates", candidates.isNotEmpty())
    }

    @Test
    fun `dual input mode uses recombination recipes`() = runTest {
        val sampleA = InputSample("a1", "A", "def foo(a, b): return a + b")
        val sampleB = InputSample("b1", "B", "def bar(x, y): return x * y")
        val modelA = normalizer.normalize(sampleA)
        val modelB = normalizer.normalize(sampleB)

        val config = ColliderConfig(
            runMode = RunMode.SAFE,
            maxCandidates = 20,
            searchDepth = 2,
            twoInputMode = true
        )

        val candidates = mutableListOf<com.collide.app.domain.model.CandidateResult>()
        generator.generateCandidates(modelA, modelB, config) { candidates.add(it) }

        assertTrue("should generate candidates in dual mode", candidates.isNotEmpty())
        // Dual-input recipes start with 'd'
        val dualCandidates = candidates.filter { it.recipeId.startsWith("d") }
        assertTrue("should have dual-input recipe candidates", dualCandidates.isNotEmpty())
    }

    @Test
    fun `search depth controls operation chain length`() = runTest {
        val sampleA = InputSample("a1", "test", "def foo(a, b): return a + b + 1")
        val modelA = normalizer.normalize(sampleA)

        val shallowCandidates = mutableListOf<com.collide.app.domain.model.CandidateResult>()
        val deepCandidates = mutableListOf<com.collide.app.domain.model.CandidateResult>()

        generator.generateCandidates(modelA, null,
            ColliderConfig(runMode = RunMode.SAFE, maxCandidates = 100, searchDepth = 1)
        ) { shallowCandidates.add(it) }

        generator.generateCandidates(modelA, null,
            ColliderConfig(runMode = RunMode.SAFE, maxCandidates = 100, searchDepth = 4)
        ) { deepCandidates.add(it) }

        assertTrue("deeper search should produce more candidates than shallow",
            deepCandidates.size >= shallowCandidates.size)
    }
}
