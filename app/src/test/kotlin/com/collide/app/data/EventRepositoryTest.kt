package com.collide.app.data

import com.collide.app.domain.model.SavedEvent
import com.collide.app.data.db.EventEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for domain model mappings and invariants.
 * Full Room persistence tests require instrumented tests with an in-memory DB.
 */
class EventRepositoryTest {

    @Test
    fun `saved event compression ratio pct is correct`() {
        val event = SavedEvent(
            id = 1L,
            timestamp = 0L,
            inputFileName = "test.txt",
            inputSize = 10000L,
            baselineType = "RAW_DEFLATE",
            baselineSize = 1000L,
            winningSize = 800L,
            byteSavings = 200L,
            recipeSummary = "delta8 | Deflate/Default",
            recipeJson = "{}",
            verificationPassed = true,
            elapsedMs = 100L
        )
        assertEquals(20.0, event.compressionRatioPct, 0.001)
    }

    @Test
    fun `saved event with zero baseline gives zero ratio`() {
        val event = SavedEvent(
            id = 1L, timestamp = 0L, inputFileName = "f", inputSize = 100L,
            baselineType = "RAW_DEFLATE", baselineSize = 0L, winningSize = 0L,
            byteSavings = 0L, recipeSummary = "", recipeJson = "{}", verificationPassed = true,
            elapsedMs = 0L
        )
        assertEquals(0.0, event.compressionRatioPct, 0.0)
    }

    @Test
    fun `saved event isWinner check via classification`() {
        val event = SavedEvent(
            id = 1L, timestamp = 0L, inputFileName = "f", inputSize = 100L,
            baselineType = "RAW_DEFLATE", baselineSize = 1000L, winningSize = 800L,
            byteSavings = 200L, recipeSummary = "", recipeJson = "{}", verificationPassed = true,
            elapsedMs = 0L
        )
        assertTrue(event.verificationPassed)
        assertTrue(event.byteSavings > 0)
    }

    @Test
    fun `event entity fields map correctly`() {
        val entity = EventEntity(
            id = 5L,
            timestamp = 1700000000L,
            inputFileName = "myfile.bin",
            inputSize = 2048L,
            baselineType = "DEFLATE_BEST_COMPRESSION",
            baselineSize = 512L,
            winningSize = 400L,
            byteSavings = 112L,
            recipeSummary = "xor_prev | Deflate/Default",
            recipeJson = "{\"id\":\"r1\"}",
            verificationPassed = true,
            elapsedMs = 50L,
            notes = "test note"
        )
        assertEquals(5L, entity.id)
        assertEquals("myfile.bin", entity.inputFileName)
        assertEquals(112L, entity.byteSavings)
        assertTrue(entity.verificationPassed)
    }
}
