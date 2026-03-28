package com.velithorne.vessel.morphogenesis

import com.velithorne.vessel.util.Smoothing

object ContourGrowthEngine {

    fun step(
        prev: ContourParams?,
        genome: SpeciesGenome,
        field: GrowthPressureField,
        acc: PressureAccumulator,
        tuning: GrowthTuning,
    ): ContourParams {
        val target = ContourParams(
            crownWidthMul = 0.92f + genome.cranialExpansionBias * 0.18f + field.cortical * 0.12f,
            thoraxWidthMul = 0.95f + field.centralChamber * 0.1f + genome.shellThickness * 0.08f,
            tailLengthMul = 0.88f + genome.lowerReservoirBias * 0.22f + acc.archive * 0.14f,
            asymmetryX = (genome.asymmetryBias * 0.06f + (acc.signal - 0.5f) * 0.04f).coerceIn(-0.08f, 0.08f),
            thermalBulge = (acc.thermal * field.perimeterShell * genome.coolingVeilBias).coerceIn(0f, 1f),
        )
        if (prev == null) return target
        val a = tuning.contourAdaptAlpha
        return ContourParams(
            crownWidthMul = Smoothing.lerp(prev.crownWidthMul, target.crownWidthMul, a),
            thoraxWidthMul = Smoothing.lerp(prev.thoraxWidthMul, target.thoraxWidthMul, a),
            tailLengthMul = Smoothing.lerp(prev.tailLengthMul, target.tailLengthMul, a),
            asymmetryX = Smoothing.lerp(prev.asymmetryX, target.asymmetryX, a),
            thermalBulge = Smoothing.lerp(prev.thermalBulge, target.thermalBulge, a),
        )
    }
}
