package com.velithorne.vessel.morphogenesis

import com.velithorne.vessel.util.Smoothing
import kotlin.math.roundToInt

object PathwayGrowthEngine {

    fun spec(
        graph: StructuralGraph,
        genome: SpeciesGenome,
        acc: PressureAccumulator,
        tuning: GrowthTuning,
        prev: PathwaySpec?,
    ): PathwaySpec {
        val metabolic = (0.35f + acc.reserve * 0.25f + genome.conduitDensityBias * 0.2f).coerceIn(0.2f, 1.2f)
        val neural = (0.32f + acc.neural * 0.35f + genome.conduitDensityBias * 0.2f).coerceIn(0.2f, 1.2f)
        val branches = (2 + acc.signal * 3f + genome.antennaBranchBias * 2f).roundToInt().coerceIn(2, 6)
        val tendon = (0.25f + acc.motion * 0.35f + genome.tendonDensityBias * 0.25f).coerceIn(0.15f, 1f)
        val raw = PathwaySpec(metabolic, neural, branches, tendon)
        if (prev == null) return raw
        val a = tuning.genomeDriftAlpha * 1.5f
        return PathwaySpec(
            metabolicThickness = Smoothing.lerp(prev.metabolicThickness, raw.metabolicThickness, a),
            neuralThickness = Smoothing.lerp(prev.neuralThickness, raw.neuralThickness, a),
            signalBranchCount = raw.signalBranchCount,
            tendonVisibility = Smoothing.lerp(prev.tendonVisibility, raw.tendonVisibility, a),
        )
    }
}
