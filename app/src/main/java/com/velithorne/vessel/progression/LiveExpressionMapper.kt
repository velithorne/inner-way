package com.velithorne.vessel.progression

import com.velithorne.vessel.physiology.PhysiologySnapshot
import kotlin.math.max

object LiveExpressionMapper {

    fun map(phys: PhysiologySnapshot): LiveExpressionState {
        val s = phys.species
        val v = s.vitality.coerceIn(0f, 1f)
        val stress = s.stress.coerceIn(0f, 1f)
        val fever = s.fever.coerceIn(0f, 1f)
        val hunger = s.hunger.coerceIn(0f, 1f)
        val rec = s.recovery.coerceIn(0f, 1f)

        val strain = (stress * 0.45f + fever * 0.35f + (1f - v) * 0.2f).coerceIn(0f, 1f)
        val label = when {
            strain < 0.28f -> "Calm"
            strain < 0.52f -> "Adaptive"
            strain < 0.72f -> "Strained"
            else -> "Stressed"
        }

        return LiveExpressionState(
            vitalityBrightnessMul = (0.65f + v * 0.35f) * (1f - stress * 0.12f),
            feverTintMul = 0.4f + fever * 0.55f,
            stressContractionMul = 1f - stress * 0.18f,
            reserveDimMul = 0.75f + (1f - hunger) * 0.2f,
            thermalAgitationMul = 0.5f + fever * 0.45f,
            shellCoherenceMul = 0.7f + rec * 0.25f - stress * 0.1f,
            crownGlowMul = 0.75f + s.neuralActivity * 0.22f,
            lateralInflationMul = 0.75f + s.signalArousal * 0.2f,
            stressShimmerMul = 0.3f + stress * 0.5f,
            conditionLabel = label,
        )
    }

    /** Combine structural base (0..1) with live expression — result still 0..1 for drawing. */
    fun blendStructuralWithLive(structural: Float, liveMul: Float): Float =
        (structural * liveMul).coerceIn(0f, 1f)
}
