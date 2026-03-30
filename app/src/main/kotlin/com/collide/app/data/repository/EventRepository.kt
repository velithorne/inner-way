package com.collide.app.data.repository

import com.collide.app.data.db.AppDatabase
import com.collide.app.data.db.EventEntity
import com.collide.app.data.db.RunSummaryEntity
import com.collide.app.domain.model.RunMode
import com.collide.app.domain.model.RunStats
import com.collide.app.domain.model.SavedEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EventRepository(db: AppDatabase) {
    private val eventDao = db.eventDao()
    private val runSummaryDao = db.runSummaryDao()

    val allEvents: Flow<List<SavedEvent>> = eventDao.getAllEvents().map { entities ->
        entities.map { it.toDomain() }
    }

    val eventCount: Flow<Int> = eventDao.getEventCount()

    suspend fun getEventById(id: Long): SavedEvent? = eventDao.getEventById(id)?.toDomain()

    suspend fun saveEvent(event: SavedEvent): Long {
        return eventDao.insertEvent(event.toEntity())
    }

    suspend fun deleteEvent(event: SavedEvent) {
        eventDao.deleteEvent(event.toEntity())
    }

    suspend fun saveRunSummary(
        fileName: String,
        inputSize: Long,
        runMode: RunMode,
        stats: RunStats,
        timestamp: Long = System.currentTimeMillis()
    ): Long {
        return runSummaryDao.insertSummary(
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
                elapsedMs = stats.elapsedMs
            )
        )
    }

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
        notes = notes
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
        notes = notes
    )
}
