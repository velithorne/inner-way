package com.collide.app.data.repository

import com.collide.app.data.db.CollideDatabase
import com.collide.app.data.db.EventEntity
import com.collide.app.data.db.NearMissEntity
import com.collide.app.data.db.RunSummaryEntity
import com.collide.app.domain.model.CandidateClassification
import com.collide.app.domain.model.CandidateResult
import com.collide.app.domain.model.ColliderConfig
import com.collide.app.domain.model.CollisionMode
import com.collide.app.domain.model.EventType
import com.collide.app.domain.model.ReplayStatus
import com.collide.app.domain.model.RunStats
import com.collide.app.domain.model.SavedEvent
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class EventRepository(private val db: CollideDatabase) {

    private val gson = Gson()

    fun getAllEvents(): Flow<List<SavedEvent>> =
        db.eventDao().getAllEvents().map { entities -> entities.map { it.toDomain() } }

    suspend fun getEventById(id: String): SavedEvent? =
        db.eventDao().getEventById(id)?.toDomain()

    fun getEventCount(): Flow<Int> = db.eventDao().getEventCount()

    suspend fun saveEvent(
        candidate: CandidateResult,
        config: ColliderConfig,
        inputLabelA: String,
        inputLabelB: String?,
        inputProfileA: String,
        inputProfileB: String?,
        collisionRecipeJson: String
    ): String {
        val id = UUID.randomUUID().toString()
        val eventType = candidate.passedDetectors()
            .firstOrNull()?.eventType ?: EventType.COHERENT_VARIANT

        val entity = EventEntity(
            id = id,
            timestamp = System.currentTimeMillis(),
            mode = if (inputLabelB != null) CollisionMode.DUAL_INPUT_RECOMBINATION.name
                   else CollisionMode.SINGLE_INPUT_MUTATION.name,
            inputLabelA = inputLabelA,
            inputLabelB = inputLabelB,
            inputProfileA = inputProfileA,
            inputProfileB = inputProfileB,
            eventType = eventType.name,
            collisionRecipeJson = collisionRecipeJson,
            candidatePreview = candidate.candidateText.take(500),
            detectorSummaryJson = gson.toJson(candidate.detectorResults.map { r ->
                mapOf("id" to r.detectorId, "name" to r.detectorName,
                    "score" to r.score, "passed" to r.passed, "reason" to r.reason)
            }),
            replayable = candidate.replayable,
            notes = candidate.evaluationNotes.take(300)
        )
        db.eventDao().insertEvent(entity)
        return id
    }

    suspend fun updateReplayStatus(eventId: String, status: ReplayStatus) {
        val entity = db.eventDao().getEventById(eventId) ?: return
        db.eventDao().updateEvent(entity.copy(replayStatus = status.name))
    }

    suspend fun deleteEvent(id: String) = db.eventDao().deleteEvent(id)

    suspend fun saveRunSummary(runId: String, stats: RunStats, mode: String) {
        db.runSummaryDao().insertRunSummary(
            RunSummaryEntity(
                id = runId,
                timestamp = System.currentTimeMillis(),
                mode = mode,
                candidatesSeen = stats.candidatesSeen,
                candidatesPruned = stats.candidatesPrunedPreEval,
                candidatesEvaluated = stats.candidatesEvaluated,
                parseFailures = stats.parseFailures,
                invalidStructureCount = stats.invalidStructureCount,
                notInterestingCount = stats.notInterestingCount,
                savedEventCount = stats.savedEventCount,
                elapsedMs = stats.elapsedMs
            )
        )
    }

    suspend fun saveNearMiss(runId: String, candidate: CandidateResult) {
        db.nearMissDao().insertNearMiss(
            NearMissEntity(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                runId = runId,
                recipeId = candidate.recipeId,
                recipeName = candidate.recipeName,
                bestScore = candidate.bestScore(),
                candidatePreview = candidate.candidateText.take(200),
                classificationReason = candidate.evaluationNotes.take(200)
            )
        )
    }

    private fun EventEntity.toDomain(): SavedEvent = SavedEvent(
        id = id,
        timestamp = timestamp,
        mode = try { CollisionMode.valueOf(mode) } catch (e: Exception) { CollisionMode.SINGLE_INPUT_MUTATION },
        inputLabelA = inputLabelA,
        inputLabelB = inputLabelB,
        inputProfileA = inputProfileA,
        inputProfileB = inputProfileB,
        eventType = try { EventType.valueOf(eventType) } catch (e: Exception) { EventType.COHERENT_VARIANT },
        collisionRecipeJson = collisionRecipeJson,
        candidatePreview = candidatePreview,
        detectorSummaryJson = detectorSummaryJson,
        replayable = replayable,
        notes = notes,
        replayStatus = try { ReplayStatus.valueOf(replayStatus) } catch (e: Exception) { ReplayStatus.NOT_REPLAYED }
    )
}
