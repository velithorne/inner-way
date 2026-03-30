package com.collide.app.data

import com.collide.app.data.db.MIGRATION_1_2
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit-level tests for the migration SQL logic (schema invariants).
 * Full instrumented migration tests require an Android device/emulator
 * and belong in androidTest. These tests validate business-rule invariants
 * and column-list completeness that don't need a real SQLite context.
 */
class DatabaseMigrationTest {

    @Test
    fun `MIGRATION_1_2 is configured from version 1 to version 2`() {
        val migration = MIGRATION_1_2
        assertEquals(1, migration.startVersion)
        assertEquals(2, migration.endVersion)
    }

    @Test
    fun `Phase 2 EventEntity has all required fields`() {
        val entity = com.collide.app.data.db.EventEntity(
            timestamp = 0L, inputFileName = "f", inputSize = 100L,
            baselineType = "RAW_DEFLATE", baselineSize = 50L, winningSize = 40L,
            byteSavings = 10L, recipeSummary = "s", recipeJson = "{}", verificationPassed = true,
            elapsedMs = 10L,
            originalSha256 = "abc", reconstructedSha256 = "abc",
            verificationMethod = "BYTE_EQUALITY_AND_SHA256",
            transformedPayloadSize = 90L, backendCompressedSize = 40L,
            transformMetadataSize = 4L, containerHeaderSize = 8L,
            candidateIndex = 3, runConfigJson = "{}", engineVersion = "2.0.0-phase2",
            replayStatus = "PENDING"
        )
        assertEquals("abc", entity.originalSha256)
        assertEquals("BYTE_EQUALITY_AND_SHA256", entity.verificationMethod)
        assertEquals(3, entity.candidateIndex)
        assertEquals("PENDING", entity.replayStatus)
        assertEquals("2.0.0-phase2", entity.engineVersion)
    }

    @Test
    fun `Phase 2 RunSummaryEntity has all required fields`() {
        val entity = com.collide.app.data.db.RunSummaryEntity(
            timestamp = 0L, fileName = "f", inputSize = 100L, runMode = "BALANCED",
            candidatesSeen = 80, candidatesPruned = 0, candidatesEvaluated = 80,
            exactnessFailures = 2, noGainCount = 70, winnerCount = 3, elapsedMs = 500L,
            hashMismatches = 1, encodeErrors = 0, decodeErrors = 0,
            metadataAccountingFailures = 0, baselineSize = 200L, bestWinnerSize = 160L,
            engineVersion = "2.0.0-phase2"
        )
        assertEquals(1, entity.hashMismatches)
        assertEquals(200L, entity.baselineSize)
        assertEquals(160L, entity.bestWinnerSize)
        assertEquals("2.0.0-phase2", entity.engineVersion)
    }

    @Test
    fun `CorpusRunSummaryEntity has required fields`() {
        val entity = com.collide.app.data.db.CorpusRunSummaryEntity(
            timestamp = 0L, runMode = "SAFE", baselineStrategy = "RAW_DEFLATE",
            totalFixtures = 7, fixturesWithWins = 3, fixturesWithoutWins = 4,
            totalWinners = 5, totalCandidatesEvaluated = 210, totalElapsedMs = 8000L,
            fixtureResultsJson = "[]"
        )
        assertEquals(7, entity.totalFixtures)
        assertEquals(3, entity.fixturesWithWins)
        assertEquals("[]", entity.fixtureResultsJson)
    }
}
