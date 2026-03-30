package com.collide.app.domain.recipes

import com.collide.app.domain.model.RunMode
import org.junit.Assert.*
import org.junit.Test

class CandidateGeneratorTest {
    private val generator = CandidateGenerator()

    @Test
    fun `safe mode does not exceed max candidates`() {
        val candidates = generator.generate(RunMode.SAFE)
        assertTrue(candidates.size <= RunMode.SAFE.maxCandidates)
    }

    @Test
    fun `balanced mode does not exceed max candidates`() {
        val candidates = generator.generate(RunMode.BALANCED)
        assertTrue(candidates.size <= RunMode.BALANCED.maxCandidates)
    }

    @Test
    fun `burst mode does not exceed max candidates`() {
        val candidates = generator.generate(RunMode.BURST)
        assertTrue(candidates.size <= RunMode.BURST.maxCandidates)
    }

    @Test
    fun `generated candidates are non-empty`() {
        val candidates = generator.generate(RunMode.BALANCED)
        assertTrue(candidates.isNotEmpty())
    }

    @Test
    fun `all candidates have at least one transform`() {
        val candidates = generator.generate(RunMode.BALANCED)
        candidates.forEach { recipe ->
            assertTrue("Recipe ${recipe.id} has no transforms", recipe.transformIds.isNotEmpty())
        }
    }

    @Test
    fun `chain lengths respect max chain length`() {
        val candidates = generator.generate(RunMode.SAFE) // maxChainLength = 2
        candidates.forEach { recipe ->
            assertTrue(
                "Recipe ${recipe.id} chain too long: ${recipe.transformIds.size}",
                recipe.transformIds.size <= RunMode.SAFE.maxChainLength
            )
        }
    }

    @Test
    fun `generation is deterministic`() {
        val c1 = generator.generate(RunMode.BALANCED)
        val c2 = generator.generate(RunMode.BALANCED)
        assertEquals(c1.size, c2.size)
        c1.zip(c2).forEach { (a, b) ->
            assertEquals(a.transformIds, b.transformIds)
            assertEquals(a.backend, b.backend)
        }
    }

    @Test
    fun `override max candidates is respected`() {
        val candidates = generator.generate(RunMode.BURST, maxCandidatesOverride = 5)
        assertTrue(candidates.size <= 5)
    }

    @Test
    fun `burst mode generates more than safe mode`() {
        val safe = generator.generate(RunMode.SAFE)
        val burst = generator.generate(RunMode.BURST)
        assertTrue(burst.size > safe.size)
    }
}
