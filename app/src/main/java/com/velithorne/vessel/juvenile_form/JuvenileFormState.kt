package com.velithorne.vessel.juvenile_form

import com.velithorne.vessel.model.BodyRegionState
import com.velithorne.vessel.model.JuvenileVisualState
import com.velithorne.vessel.model.TopologyExpansionState

/**
 * Full juvenile layer output for renderer + UI.
 */
data class JuvenileFormState(
    val active: Boolean,
    val bodyPlan: JuvenileBodyPlan,
    val traits: JuvenileTraitMap,
    val transition: JuvenileTransitionState,
    val regions: BodyRegionState,
    val expansion: TopologyExpansionState,
    val visual: JuvenileVisualState,
)
