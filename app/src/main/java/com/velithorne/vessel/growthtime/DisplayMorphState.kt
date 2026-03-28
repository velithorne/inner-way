package com.velithorne.vessel.growthtime

import com.velithorne.vessel.morphogenesis.BodyMassFieldState
import com.velithorne.vessel.morphogenesis.BuddingStructure
import com.velithorne.vessel.morphogenesis.ChamberMassModel
import com.velithorne.vessel.morphogenesis.ContourParams
import com.velithorne.vessel.morphogenesis.GrowthVisualCues
import com.velithorne.vessel.morphogenesis.SeedCore

/**
 * Lagging visible morphology — what the renderer uses after [MorphLagEngine].
 */
data class DisplayMorphState(
    val seedFormBlend: Float,
    val contour: ContourParams,
    val seedCore: SeedCore,
    val chamberMass: ChamberMassModel,
    val bodyMass: BodyMassFieldState,
    val budding: BuddingStructure,
    val growthVisuals: GrowthVisualCues,
    /** Growth-front leads structure: 1 = full front shimmer, structure catches up slower. */
    val growthFrontLead: Float,
    val temporalStage: TemporalGrowthStage,
    val lastWallClockMs: Long,
)
