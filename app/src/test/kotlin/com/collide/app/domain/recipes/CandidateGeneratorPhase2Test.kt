package com.collide.app.domain.recipes

import com.collide.app.domain.model.RunMode
import org.junit.Assert.*
import org.junit.Test

class CandidateGeneratorPhase2Test {

    @Test
    fun `enabled transform filter restricts candidates`() {
        val enabled = setOf("delta8", "xor_prev")
        val gen = CandidateGenerator(enabled)
        val candidates = gen.generate(RunMode.BURST)
        candidates.forEach { recipe ->
            recipe.transformIds.forEach { id ->
                assertTrue("Transform $id should be in enabled set", id in enabled)
            }
        }
    }

    @Test
    fun `empty enabled set uses all transforms`() {
        val genAll = CandidateGenerator(emptySet())
        val genSome = CandidateGenerator(setOf("delta8"))
        val all = genAll.generate(RunMode.BALANCED, maxCandidatesOverride = 200)
        val some = genSome.generate(RunMode.BALANCED, maxCandidatesOverride = 200)
        assertTrue("All-enabled should produce more candidates", all.size > some.size)
    }

    @Test
    fun `chain length 4 produces longer chains`() {
        val gen = CandidateGenerator(emptySet())
        val candidates = gen.generate(RunMode.BURST, maxCandidatesOverride = 1000, maxChainLengthOverride = 4)
        val has4 = candidates.any { it.transformIds.size == 4 }
        // With enough candidates, there should be some 4-length chains
        assertTrue("Should have at least some 4-length chains with override 4", has4)
    }

    @Test
    fun `no consecutive duplicate transforms in any chain`() {
        val gen = CandidateGenerator(emptySet())
        val candidates = gen.generate(RunMode.BURST, maxCandidatesOverride = 500, maxChainLengthOverride = 4)
        candidates.forEach { recipe ->
            for (i in 0 until recipe.transformIds.size - 1) {
                assertNotEquals(
                    "Recipe ${recipe.id} has consecutive duplicate at position $i",
                    recipe.transformIds[i],
                    recipe.transformIds[i + 1]
                )
            }
        }
    }

    @Test
    fun `generation remains deterministic with same enabled set`() {
        val enabled = setOf("delta8", "xor_prev", "move_to_front")
        val gen = CandidateGenerator(enabled)
        val c1 = gen.generate(RunMode.BALANCED)
        val c2 = gen.generate(RunMode.BALANCED)
        assertEquals(c1.size, c2.size)
        c1.zip(c2).forEach { (a, b) ->
            assertEquals(a.transformIds, b.transformIds)
            assertEquals(a.backend, b.backend)
        }
    }
}
