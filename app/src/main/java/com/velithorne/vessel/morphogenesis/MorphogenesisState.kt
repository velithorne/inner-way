package com.velithorne.vessel.morphogenesis

/**
 * Mutable engine state — updated incrementally each physiology tick.
 */
data class MorphogenesisState(
    val seed: SpeciesSeed,
    val genome: SpeciesGenome,
    val accumulator: PressureAccumulator,
    val field: GrowthPressureField,
    val graph: StructuralGraph,
    /** Normalized contour control: crown width, thorax width, tail length, asymmetry */
    val contourParams: ContourParams,
    val tissue: TissueEnvelopeState,
    val pathwaySpec: PathwaySpec,
    val growthPhase: Float,
    val lastEvents: List<GrowthEvent>,
)

data class ContourParams(
    val crownWidthMul: Float,
    val thoraxWidthMul: Float,
    val tailLengthMul: Float,
    val asymmetryX: Float,
    val thermalBulge: Float,
)

data class TissueEnvelopeState(
    val shellOpacityMul: Float,
    val gelEnvelopeMul: Float,
    val archiveLamellaDensity: Float,
    val coolingVeilStrength: Float,
)

data class PathwaySpec(
    val metabolicThickness: Float,
    val neuralThickness: Float,
    val signalBranchCount: Int,
    val tendonVisibility: Float,
)
