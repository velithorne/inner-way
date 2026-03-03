package com.falcor.civilization.engine.analysis

import com.falcor.civilization.domain.ObservationPoint
import com.falcor.civilization.domain.PreregPlanContent
import com.falcor.civilization.engine.agents.AuditorAgent

data class PipelineResult(
    val decision: String,
    val metrics: Map<String, Double>,
    val rawPValues: Map<String, Double>,
    val correctedPValues: Map<String, Double>,
    val effectSizes: Map<String, Double>,
    val posteriorOdds: Double,
    val integrityPassed: Boolean,
    val auditReasons: List<String>
)

class AnalysisPipeline(
    private val seed: Long,
    private val preregPlan: PreregPlanContent
) {
    private val auditor = AuditorAgent()

    fun run(observations: List<ObservationPoint>): PipelineResult {
        val features = FeatureExtractor.extract(observations)
        val metrics = mapOf(
            "slope_temp_ir" to features.slopeTempIr,
            "slope_rpm" to features.slopeRpm,
            "slope_em_amp" to features.slopeEmAmp,
            "delta_em_amp" to features.deltaEmAmp,
            "fft_peak_rpm" to features.fftPeakRpm,
            "cross_corr_temp_rpm" to features.crossCorrTempRpm,
            "cross_corr_rpm_em" to features.crossCorrRpmEm,
            "median_vib_rms" to features.medianVibRms
        )

        val rng = java.util.Random(seed)
        val nullSlopes = (0 until 100).map { rng.nextGaussian() * 0.01 }
        val pSlopeTemp = HypothesisTest.permutationTest(features.slopeTempIr, nullSlopes, seed)
        val pSlopeRpm = HypothesisTest.permutationTest(features.slopeRpm, nullSlopes, seed)
        val pSlopeEm = HypothesisTest.permutationTest(features.slopeEmAmp, nullSlopes, seed)
        val pDeltaEm = HypothesisTest.permutationTest(features.deltaEmAmp, nullSlopes, seed)
        val pCrossCorr = HypothesisTest.permutationTest(features.crossCorrTempRpm, nullSlopes, seed)
        val pCrossCorrRpmEm = HypothesisTest.permutationTest(features.crossCorrRpmEm, nullSlopes, seed)

        val rawPValues = mapOf(
            "slope_temp_ir" to pSlopeTemp,
            "slope_rpm" to pSlopeRpm,
            "slope_em_amp" to pSlopeEm,
            "delta_em_amp" to pDeltaEm,
            "cross_corr_temp_rpm" to pCrossCorr,
            "cross_corr_rpm_em" to pCrossCorrRpmEm
        )

        val preregMetrics = preregPlan.metrics.filter { rawPValues.containsKey(it) }
        val relevantPValues = preregMetrics.associateWith { rawPValues[it]!! }
        val auditResult = auditor.audit(
            observations,
            relevantPValues,
            preregPlan.minSampleSize,
            preregPlan.alpha
        )

        val correctedPValues = auditResult.correctedPValues ?: rawPValues
        val minCorrectedP = correctedPValues.values.minOrNull() ?: 1.0
        val effectSize = if (preregMetrics.isNotEmpty()) {
            val es = preregMetrics.mapNotNull { metrics[it] }.average().let { kotlin.math.abs(it) }
            es
        } else 0.0

        val posteriorOdds = BayesianUpdate.posteriorOdds(
            priorOdds = 0.5,
            pValue = minCorrectedP,
            effectSize = effectSize,
            minEffectSize = preregPlan.thresholds.values.minOrNull() ?: 0.2
        )

        val passesThreshold = minCorrectedP <= preregPlan.alpha &&
                effectSize >= (preregPlan.thresholds.values.minOrNull() ?: 0.0) &&
                auditResult.passed

        val decision = when {
            !auditResult.passed -> "REJECT_INTEGRITY"
            minCorrectedP > preregPlan.alpha -> "FAIL_NULL"
            effectSize < (preregPlan.thresholds.values.minOrNull() ?: 0.0) -> "FAIL_EFFECT"
            else -> "PROMOTE"
        }

        val effectSizes = preregMetrics.associateWith { metrics[it] ?: 0.0 }

        return PipelineResult(
            decision = decision,
            metrics = metrics,
            rawPValues = rawPValues,
            correctedPValues = correctedPValues,
            effectSizes = effectSizes,
            posteriorOdds = posteriorOdds,
            integrityPassed = auditResult.passed,
            auditReasons = auditResult.reasons
        )
    }
}
