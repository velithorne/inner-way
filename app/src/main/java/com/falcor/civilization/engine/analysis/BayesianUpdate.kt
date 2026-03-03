package com.falcor.civilization.engine.analysis

import kotlin.math.*

/**
 * Simple Bayesian evidence update. Posterior odds for effect vs artifact.
 */
object BayesianUpdate {
    /**
     * Log-odds update: log(p/(1-p)) after observing evidence.
     * Prior odds for effect, likelihood ratio from p-value.
     */
    fun posteriorOdds(
        priorOdds: Double,
        pValue: Double,
        effectSize: Double,
        minEffectSize: Double = 0.2
    ): Double {
        val lr = likelihoodRatio(pValue, effectSize, minEffectSize)
        val priorLogOdds = ln(priorOdds / (1 + priorOdds))
        val posteriorLogOdds = priorLogOdds + ln(lr)
        return exp(posteriorLogOdds) / (1 - exp(posteriorLogOdds))
    }

    private fun likelihoodRatio(pValue: Double, effectSize: Double, minEffect: Double): Double {
        if (pValue >= 0.05) return 0.5
        val pFactor = 1.0 / max(pValue, 0.001)
        val effectFactor = if (effectSize >= minEffect) 2.0 else 1.0
        return min(pFactor * effectFactor, 100.0)
    }
}
