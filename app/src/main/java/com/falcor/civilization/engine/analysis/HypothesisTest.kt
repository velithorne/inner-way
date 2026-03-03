package com.falcor.civilization.engine.analysis

import com.falcor.civilization.engine.analysis.FeatureExtractor.ExtractedFeatures
import kotlin.math.*

/**
 * Permutation test for slope difference. Deterministic given seed.
 */
object HypothesisTest {
    fun permutationTest(
        observedSlope: Double,
        nullSlopes: List<Double>,
        seed: Long,
        nPermutations: Int = 1000
    ): Double {
        val rng = java.util.Random(seed)
        val combined = nullSlopes + observedSlope
        var count = 0
        repeat(nPermutations) {
            val shuffled = combined.shuffled(rng)
            val permSlope = shuffled.last()
            val permNull = shuffled.dropLast(1)
            val permDiff = abs(permSlope - permNull.average())
            val obsDiff = abs(observedSlope - nullSlopes.average())
            if (permDiff >= obsDiff) count++
        }
        return (count + 1).toDouble() / (nPermutations + 1)
    }

    fun cohensD(group1: List<Double>, group2: List<Double>): Double {
        val n1 = group1.size
        val n2 = group2.size
        if (n1 < 2 || n2 < 2) return 0.0
        val m1 = group1.average()
        val m2 = group2.average()
        val v1 = group1.map { (it - m1).pow(2) }.sum() / (n1 - 1)
        val v2 = group2.map { (it - m2).pow(2) }.sum() / (n2 - 1)
        val pooledStd = sqrt(((n1 - 1) * v1 + (n2 - 1) * v2) / (n1 + n2 - 2))
        return if (pooledStd < 1e-10) 0.0 else (m1 - m2) / pooledStd
    }

    fun robustMedianDiff(group1: List<Double>, group2: List<Double>): Double {
        val m1 = group1.sorted().let { it[it.size / 2] }
        val m2 = group2.sorted().let { it[it.size / 2] }
        return m1 - m2
    }
}
