package com.velithorne.vessel.morphogenesis

/**
 * Immutable frame output for UI + renderer mapping.
 */
data class MorphogenesisSnapshot(
    val timestampMillis: Long,
    val seed: SpeciesSeed,
    val genome: SpeciesGenome,
    val pressures: GrowthPressure,
    val accumulated: PressureAccumulator,
    val field: GrowthPressureField,
    val graph: StructuralGraph,
    val contour: ContourParams,
    val tissue: TissueEnvelopeState,
    val pathways: PathwaySpec,
    /** Crystalline seed nucleus + germination state. */
    val seedCore: SeedCore,
    val chamberMass: ChamberMassModel,
    val bodyMass: BodyMassFieldState,
    val growthFront: GrowthFront,
    val budding: BuddingStructure,
    val organEmbedding: OrganEmbeddingFactors,
    val germinationStage: GerminationStage,
    /** 0..1 visible growth shimmer intensity */
    val visibleGrowthActivity: Float,
    val growthStatusLabel: String,
    val growthStatusLine: String,
    val explainerLines: List<String>,
)
