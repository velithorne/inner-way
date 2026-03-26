package com.velithorne.vessel.growthtime

import com.velithorne.vessel.morphogenesis.BodyMassFieldState
import com.velithorne.vessel.morphogenesis.BuddingStructure
import com.velithorne.vessel.morphogenesis.ChamberMassModel
import com.velithorne.vessel.morphogenesis.ContourParams
import com.velithorne.vessel.morphogenesis.GerminationStage
import com.velithorne.vessel.morphogenesis.GrowthVisualCues
import com.velithorne.vessel.morphogenesis.SeedCore

/**
 * Lagging display fields merged into [com.velithorne.vessel.morphogenesis.MorphogenesisSnapshot] for rendering.
 */
data class MorphogenesisDisplayOverride(
    val seedFormBlend: Float,
    val contour: ContourParams,
    val seedCore: SeedCore,
    val chamberMass: ChamberMassModel,
    val bodyMass: BodyMassFieldState,
    val budding: BuddingStructure,
    val growthVisuals: GrowthVisualCues,
    val growthFrontLead: Float,
    /** UI / explainer stage — mapped from temporal progression. */
    val displayedGerminationStage: GerminationStage,
)

object TemporalToGerminationStage {
    fun map(t: TemporalGrowthStage): GerminationStage = when (t) {
        TemporalGrowthStage.DORMANT_SEED -> GerminationStage.DORMANT_SEED
        TemporalGrowthStage.ACTIVATED_SEED -> GerminationStage.ACTIVATED_SEED
        TemporalGrowthStage.GERMINATING -> GerminationStage.GERMINATING
        TemporalGrowthStage.CROWN_FORMING -> GerminationStage.CHAMBER_FORMATION
        TemporalGrowthStage.LATERAL_BUDDING -> GerminationStage.BRANCHING
        TemporalGrowthStage.CHAMBER_DEEPENING -> GerminationStage.RESERVOIR_DEEPENING
        TemporalGrowthStage.SHELL_ACCRETING -> GerminationStage.SHELL_THICKENING
        TemporalGrowthStage.STABILIZING -> GerminationStage.STABILIZING
    }
}
