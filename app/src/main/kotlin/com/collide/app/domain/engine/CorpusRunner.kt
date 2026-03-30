package com.collide.app.domain.engine

import com.collide.app.domain.evaluator.BaselineEvaluator
import com.collide.app.domain.evaluator.CandidateEvaluator
import com.collide.app.domain.model.*
import com.collide.app.domain.recipes.CandidateGenerator
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

/**
 * Runs the collider across all provided fixtures and produces a CorpusSummary.
 *
 * Each fixture is evaluated independently with the same run configuration.
 * Results are collected honestly — wins, losses, and failures all reported.
 */
class CorpusRunner(
    private val generator: CandidateGenerator = CandidateGenerator(),
    private val evaluator: CandidateEvaluator = CandidateEvaluator(),
    private val baselineEvaluator: BaselineEvaluator = BaselineEvaluator()
) {

    data class CorpusRunConfig(
        val runMode: RunMode,
        val baselineStrategy: BaselineStrategy,
        val maxCandidatesOverride: Int? = null,
        val maxChainLengthOverride: Int? = null
    )

    suspend fun run(
        fixtures: List<InputSample>,
        config: CorpusRunConfig,
        onFixtureProgress: (fixtureName: String, fixtureIndex: Int) -> Unit = { _, _ -> }
    ): CorpusSummary {
        val startTime = System.currentTimeMillis()
        val fixtureResults = mutableListOf<FixtureResult>()

        val candidates = generator.generate(
            config.runMode,
            config.maxCandidatesOverride,
            config.maxChainLengthOverride
        )

        for ((fixtureIndex, fixture) in fixtures.withIndex()) {
            if (!coroutineContext.isActive) break

            onFixtureProgress(fixture.fileName, fixtureIndex)

            val baseline = baselineEvaluator.evaluate(fixture.bytes, config.baselineStrategy)
            var winnersFound = 0
            var bestWinnerSize: Long? = null
            var bestSavings = 0L
            var evaluated = 0
            var exactnessFails = 0
            var notApplicable = 0
            var noGain = 0
            val fixtureStart = System.currentTimeMillis()

            for ((idx, recipe) in candidates.withIndex()) {
                if (!coroutineContext.isActive) break
                val result = evaluator.evaluate(fixture.bytes, recipe, baseline.encodedSize, idx)
                evaluated++
                when (result.classification) {
                    CandidateClassification.STRICT_WINNER -> {
                        winnersFound++
                        if (bestWinnerSize == null || result.encodedSize < bestWinnerSize!!) {
                            bestWinnerSize = result.encodedSize
                            bestSavings = result.byteSavings
                        }
                    }
                    CandidateClassification.EXACTNESS_FAILED, CandidateClassification.HASH_MISMATCH -> exactnessFails++
                    CandidateClassification.NOT_APPLICABLE -> notApplicable++
                    CandidateClassification.NO_STRICT_IMPROVEMENT -> noGain++
                    else -> {}
                }
            }

            fixtureResults.add(FixtureResult(
                fixtureName = fixture.fileName,
                fixtureSize = fixture.size.toLong(),
                baselineSize = baseline.encodedSize,
                winnersFound = winnersFound,
                bestWinnerSize = bestWinnerSize,
                bestSavings = bestSavings,
                candidatesEvaluated = evaluated,
                exactnessFailures = exactnessFails,
                notApplicableCount = notApplicable,
                noGainCount = noGain,
                elapsedMs = System.currentTimeMillis() - fixtureStart
            ))
        }

        return CorpusSummary(
            timestamp = System.currentTimeMillis(),
            runMode = config.runMode.name,
            baselineStrategy = config.baselineStrategy.name,
            fixtureResults = fixtureResults,
            totalElapsedMs = System.currentTimeMillis() - startTime
        )
    }
}
