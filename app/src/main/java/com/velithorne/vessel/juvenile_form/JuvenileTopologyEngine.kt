package com.velithorne.vessel.juvenile_form

import com.velithorne.vessel.model.TopologyExpansionState
import com.velithorne.vessel.model.VisibleMorphologyState
import kotlin.math.abs

object JuvenileTopologyEngine {

    fun expand(
        plan: JuvenileBodyPlan,
        visible: VisibleMorphologyState,
        tuning: JuvenileTuning,
        emergence: Float,
    ): TopologyExpansionState {
        val div = tuning.topologyDivergenceStrength * emergence
        val upper = (plan.crownMass - 0.45f) * div + visible.crownChamberNy.let { (0.5f - it) * 0.4f }
        val lower = (plan.reserveMass - 0.45f) * div + (visible.reserveBasinNy - 0.55f) * 0.5f
        val lateral = abs(visible.frondRootRightNx - visible.frondRootLeftNx) * 0.5f * div
        val exp = (
            emergence * 0.55f +
                (plan.lateralMass + plan.crownMass + plan.reserveMass) / 3f * 0.25f * div +
                visible.visibleAsymmetryScore * 0.2f
            ).coerceIn(0f, 1f)
        return TopologyExpansionState(
            expansionFactor = exp,
            upperBias = upper.coerceIn(-0.55f, 0.55f),
            lowerBias = lower.coerceIn(-0.55f, 0.55f),
            lateralAsymmetry = lateral.coerceIn(0f, 0.65f),
        )
    }
}
