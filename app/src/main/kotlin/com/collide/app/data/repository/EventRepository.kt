package com.collide.app.data.repository

import com.collide.app.data.db.AppDatabase
import com.collide.app.data.db.EventEntity
import com.collide.app.data.db.RunSummaryEntity
import com.collide.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EventRepository(db: AppDatabase) {
    private val eventDao = db.eventDao()
    private val runSummaryDao = db.runSummaryDao()

    val allEvents: Flow<List<SavedEvent>> = eventDao.getAllEvents().map { list ->
        list.map { it.toDomain() }
    }

    val eventCount: Flow<Int> = eventDao.getEventCount()

    suspend fun getEventById(id: Long): SavedEvent? = eventDao.getEventById(id)?.toDomain()

    suspend fun saveEvent(event: SavedEvent): Long = eventDao.insertEvent(event.toEntity())

    suspend fun deleteEvent(event: SavedEvent) = eventDao.deleteEvent(event.toEntity())

    suspend fun updateReplayStatus(eventId: Long, status: ReplayStatus) {
        val entity = eventDao.getEventById(eventId) ?: return
        eventDao.insertEvent(entity.copy(replayStatus = status.name))
    }

    suspend fun saveRunSummary(
        fileName: String,
        inputSize: Long,
        runMode: RunMode,
        stats: RunStats,
        baselineSize: Long = 0L,
        bestWinnerSize: Long = 0L,
        timestamp: Long = System.currentTimeMillis()
    ): Long = runSummaryDao.insertSummary(
        RunSummaryEntity(
            timestamp = timestamp,
            fileName = fileName,
            inputSize = inputSize,
            runMode = runMode.name,
            candidatesSeen = stats.candidatesSeen,
            candidatesPruned = stats.candidatesPrunedPreEval,
            candidatesEvaluated = stats.candidatesEvaluated,
            exactnessFailures = stats.exactnessFailures,
            noGainCount = stats.noGainCount,
            winnerCount = stats.strictWinnerCount,
            elapsedMs = stats.elapsedMs,
            hashMismatches = stats.hashMismatches,
            encodeErrors = stats.encodeErrors,
            decodeErrors = stats.decodeErrors,
            metadataAccountingFailures = stats.metadataAccountingFailures,
            baselineSize = baselineSize,
            bestWinnerSize = bestWinnerSize,
            engineVersion = RunConfigSnapshot.ENGINE_VERSION
        )
    )

    // ── Mapping ──────────────────────────────────────────────────────────────

    private fun EventEntity.toDomain() = SavedEvent(
        id = id,
        timestamp = timestamp,
        inputFileName = inputFileName,
        inputSize = inputSize,
        baselineType = baselineType,
        baselineSize = baselineSize,
        winningSize = winningSize,
        byteSavings = byteSavings,
        recipeSummary = recipeSummary,
        recipeJson = recipeJson,
        verificationPassed = verificationPassed,
        elapsedMs = elapsedMs,
        notes = notes,
        originalSha256 = originalSha256,
        reconstructedSha256 = reconstructedSha256,
        verificationMethod = verificationMethod,
        transformedPayloadSize = transformedPayloadSize,
        backendCompressedSize = backendCompressedSize,
        transformMetadataSize = transformMetadataSize,
        containerHeaderSize = containerHeaderSize,
        candidateIndex = candidateIndex,
        runConfigJson = runConfigJson,
        engineVersion = engineVersion,
        replayStatus = replayStatus
    )

    private fun SavedEvent.toEntity() = EventEntity(
        id = id,
        timestamp = timestamp,
        inputFileName = inputFileName,
        inputSize = inputSize,
        baselineType = baselineType,
        baselineSize = baselineSize,
        winningSize = winningSize,
        byteSavings = byteSavings,
        recipeSummary = recipeSummary,
        recipeJson = recipeJson,
        verificationPassed = verificationPassed,
        elapsedMs = elapsedMs,
        notes = notes,
        originalSha256 = originalSha256,
        reconstructedSha256 = reconstructedSha256,
        verificationMethod = verificationMethod,
        transformedPayloadSize = transformedPayloadSize,
        backendCompressedSize = backendCompressedSize,
        transformMetadataSize = transformMetadataSize,
        containerHeaderSize = containerHeaderSize,
        candidateIndex = candidateIndex,
        runConfigJson = runConfigJson,
        engineVersion = engineVersion,
        replayStatus = replayStatus
    )
}
