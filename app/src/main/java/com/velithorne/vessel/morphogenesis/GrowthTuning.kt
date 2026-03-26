package com.velithorne.vessel.morphogenesis

/**
 * Morphogenesis rates and thresholds — single place for tuning.
 */
data class GrowthTuning(
    val pressureSmoothingAlpha: Float = 0.08f,
    val fieldDiffuseAlpha: Float = 0.06f,
    val genomeDriftAlpha: Float = 0.012f,
    val contourAdaptAlpha: Float = 0.045f,
    val organDriftAlpha: Float = 0.035f,
    val pathwayBranchThreshold: Float = 0.62f,
    val shellThickenThermal: Float = 0.55f,
    val archiveExpandLoad: Float = 0.48f,
    val lateralSpreadSignal: Float = 0.52f,
    val cranialExpandNeural: Float = 0.45f,
    val visibleGrowthPulseScale: Float = 0.85f,
    val explanationPressureFloor: Float = 0.28f,
    // Seed germination & tissue accretion
    val seedCoreRadiusMin: Float = 0.045f,
    val seedCoreRadiusMax: Float = 0.11f,
    val tissueAccretionAlpha: Float = 0.055f,
    val germinationProgressAlpha: Float = 0.04f,
    val growthFrontIntensityScale: Float = 0.92f,
    val chamberFillDensityScale: Float = 0.88f,
    val buddingThreshold: Float = 0.38f,
    val shellThickenRate: Float = 0.022f,
    val archiveLamellaDensityRate: Float = 0.018f,
    val crownBloomRate: Float = 0.02f,
    val frondExtensionRate: Float = 0.024f,
    val reserveSacContractionRate: Float = 0.03f,
    val translucencyMembrane: Float = 0.72f,
    val translucencyChamber: Float = 0.58f,
    val translucencyArchive: Float = 0.65f,
    /** Body mass field weights */
    val bodyMassSeedWeight: Float = 0.28f,
    val bodyMassGenomeShellWeight: Float = 0.18f,
    val bodyMassBlendBase: Float = 0.08f,
    /** Organ embedding multipliers */
    val organEmbedBase: Float = 0.08f,
    val organEmbedNeuralMul: Float = 1.1f,
    val organEmbedArchiveMul: Float = 1.05f,
    val organEmbedSignalMul: Float = 1f,
)
