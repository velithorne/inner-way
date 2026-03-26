package com.velithorne.vessel.model

import androidx.compose.ui.graphics.Color

/**
 * Physiology-driven colors for Phase 5. Built from live [com.velithorne.vessel.physiology.PhysiologySnapshot];
 * not arbitrary decoration.
 */
data class VesselPaletteState(
    val shellBase: Color,
    val shellEdge: Color,
    val shellRimCool: Color,
    val innerChamberShadow: Color,
    val metabolicPathway: Color,
    val neuralPathway: Color,
    val heartCore: Color,
    val heartRing: Color,
    val cortexNode: Color,
    val cortexFilament: Color,
    val gelMedium: Color,
    val gelGrain: Color,
    val archivePlate: Color,
    val archiveDeep: Color,
    val lungFrond: Color,
    val musculatureTension: Color,
    val thermalHot: Color,
    val thermalEdge: Color,
    val recoverySheen: Color,
    val accentSignal: Color,
    val chamberMist: Color,
)
