package com.falcor.civilization.engine.analysis

/**
 * Benjamini-Hochberg FDR correction for multiple comparisons.
 */
object BenjaminiHochberg {
    fun correct(pValues: List<Double>, alpha: Double = 0.05): List<Double> {
        if (pValues.isEmpty()) return emptyList()
        val n = pValues.size
        val indexed = pValues.mapIndexed { i, p -> i to p }.sortedBy { it.second }
        val corrected = DoubleArray(n)
        for ((rank, (origIdx, p)) in indexed.withIndex()) {
            val q = (rank + 1).toDouble() / n * alpha
            corrected[origIdx] = minOf(p * n / (rank + 1), 1.0)
        }
        return corrected.toList()
    }

    fun rejectNull(correctedPValues: List<Double>, alpha: Double): List<Boolean> {
        return correctedPValues.map { it <= alpha }
    }
}
