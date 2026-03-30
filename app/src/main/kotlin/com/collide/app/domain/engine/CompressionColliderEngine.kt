package com.collide.app.domain.engine

import com.collide.app.domain.detector.EventRecorder
import com.collide.app.domain.evaluator.BaselineEvaluator
import com.collide.app.domain.evaluator.CandidateEvaluator
import com.collide.app.domain.model.*
import com.collide.app.domain.recipes.CandidateGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

data class RunProgress(
    val fileName: String = "",
    val candidatesGenerated: Int = 0,
    val candidatesPruned: Int = 0,
    val candidatesEvaluated: Int = 0,
    val exactnessFailures: Int = 0,
    val noGainResults: Int = 0,
    val winnersFound: Int = 0,
    val bestSizeSoFar: Long = Long.MAX_VALUE,
    val baselineSize: Long = 0L,
    val elapsedMs: Long = 0L,
    val currentRecipe: String = "",
    val status: RunStatus = RunStatus.IDLE
)

enum class RunStatus {
    IDLE, RUNNING, CANCELLED, COMPLETED, ERROR
}

data class RunResult(
    val stats: RunStats,
    val winners: List<SavedEvent>,
    val baselineResult: BaselineResult
)

class CompressionColliderEngine(
    private val generator: CandidateGenerator = CandidateGenerator(),
    private val evaluator: CandidateEvaluator = CandidateEvaluator(),
    private val baselineEvaluator: BaselineEvaluator = BaselineEvaluator(),
    private val eventRecorder: EventRecorder = EventRecorder()
) {
    private val _progress = MutableStateFlow(RunProgress())
    val progress: StateFlow<RunProgress> = _progress

    suspend fun run(
        input: InputSample,
        runMode: RunMode,
        baselineStrategy: BaselineStrategy,
        maxCandidatesOverride: Int? = null,
        maxChainLengthOverride: Int? = null
    ): RunResult {
        val startTime = System.currentTimeMillis()

        _progress.value = RunProgress(
            fileName = input.fileName,
            status = RunStatus.RUNNING,
            baselineSize = 0L
        )

        // Compute baseline
        val baseline = baselineEvaluator.evaluate(input.bytes, baselineStrategy)

        _progress.value = _progress.value.copy(baselineSize = baseline.encodedSize)

        // Generate candidates
        val candidates = generator.generate(runMode, maxCandidatesOverride, maxChainLengthOverride)

        _progress.value = _progress.value.copy(
            candidatesGenerated = candidates.size,
            baselineSize = baseline.encodedSize
        )

        var stats = RunStats(candidatesSeen = candidates.size)
        val winners = mutableListOf<SavedEvent>()
        var bestSize = baseline.encodedSize

        for (recipe in candidates) {
            if (!coroutineContext.isActive) {
                _progress.value = _progress.value.copy(
                    status = RunStatus.CANCELLED,
                    elapsedMs = System.currentTimeMillis() - startTime
                )
                break
            }

            _progress.value = _progress.value.copy(
                currentRecipe = recipe.summary(),
                elapsedMs = System.currentTimeMillis() - startTime
            )

            val result = evaluator.evaluate(input.bytes, recipe, baseline.encodedSize)
            stats = stats.withResult(result)

            if (result.isWinner) {
                if (result.encodedSize < bestSize) {
                    bestSize = result.encodedSize
                }
                val event = eventRecorder.maybeCreateEvent(result, input, baseline)
                if (event != null) winners.add(event)
            }

            _progress.value = _progress.value.copy(
                candidatesPruned = stats.candidatesPrunedPreEval,
                candidatesEvaluated = stats.candidatesEvaluated,
                exactnessFailures = stats.exactnessFailures,
                noGainResults = stats.noGainCount,
                winnersFound = stats.strictWinnerCount,
                bestSizeSoFar = bestSize,
                elapsedMs = System.currentTimeMillis() - startTime
            )
        }

        val finalElapsed = System.currentTimeMillis() - startTime
        val finalStats = stats.copy(elapsedMs = finalElapsed)

        if (_progress.value.status == RunStatus.RUNNING) {
            _progress.value = _progress.value.copy(
                status = RunStatus.COMPLETED,
                elapsedMs = finalElapsed
            )
        }

        return RunResult(finalStats, winners, baseline)
    }

    fun resetProgress() {
        _progress.value = RunProgress()
    }
}
