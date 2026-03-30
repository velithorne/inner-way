package com.collide.app.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from Phase 1 schema (version 1) to Phase 2 schema (version 2).
 *
 * Adds Phase 2 columns to `events` and `run_summaries` tables,
 * and creates the new `corpus_run_summaries` table.
 * All new columns have safe DEFAULT values so existing rows remain valid.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // ── events table: Phase 2 additions ───────────────────────────────────
        db.execSQL("ALTER TABLE events ADD COLUMN originalSha256 TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE events ADD COLUMN reconstructedSha256 TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE events ADD COLUMN verificationMethod TEXT NOT NULL DEFAULT 'BYTE_EQUALITY_ONLY'")
        db.execSQL("ALTER TABLE events ADD COLUMN transformedPayloadSize INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE events ADD COLUMN backendCompressedSize INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE events ADD COLUMN transformMetadataSize INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE events ADD COLUMN containerHeaderSize INTEGER NOT NULL DEFAULT 8")
        db.execSQL("ALTER TABLE events ADD COLUMN candidateIndex INTEGER NOT NULL DEFAULT -1")
        db.execSQL("ALTER TABLE events ADD COLUMN runConfigJson TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE events ADD COLUMN engineVersion TEXT NOT NULL DEFAULT '1.0.0-phase1'")
        db.execSQL("ALTER TABLE events ADD COLUMN replayStatus TEXT NOT NULL DEFAULT 'PENDING'")

        // ── run_summaries table: Phase 2 additions ────────────────────────────
        db.execSQL("ALTER TABLE run_summaries ADD COLUMN hashMismatches INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE run_summaries ADD COLUMN encodeErrors INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE run_summaries ADD COLUMN decodeErrors INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE run_summaries ADD COLUMN metadataAccountingFailures INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE run_summaries ADD COLUMN baselineSize INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE run_summaries ADD COLUMN bestWinnerSize INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE run_summaries ADD COLUMN engineVersion TEXT NOT NULL DEFAULT '1.0.0-phase1'")

        // ── corpus_run_summaries table: new in Phase 2 ────────────────────────
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS corpus_run_summaries (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                timestamp INTEGER NOT NULL,
                runMode TEXT NOT NULL,
                baselineStrategy TEXT NOT NULL,
                totalFixtures INTEGER NOT NULL,
                fixturesWithWins INTEGER NOT NULL,
                fixturesWithoutWins INTEGER NOT NULL,
                totalWinners INTEGER NOT NULL,
                totalCandidatesEvaluated INTEGER NOT NULL,
                totalElapsedMs INTEGER NOT NULL,
                fixtureResultsJson TEXT NOT NULL
            )
        """.trimIndent())
    }
}
