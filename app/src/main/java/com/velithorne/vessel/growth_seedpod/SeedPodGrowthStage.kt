package com.velithorne.vessel.growth_seedpod

/**
 * Structural stages — ordinal order is stable for Room; append only for migrations.
 */
enum class SeedPodGrowthStage {
    DORMANT_POD,
    ACTIVATING_POD,
    GERMINATING_POD,
    EARLY_BUDDING,
    EARLY_CHAMBERING,
    CHAMBER_MATURED,
    LINEAGE_DIFFERENTIATING,
    FIRST_BRANCH_FORMING,
    /** Morphology stabilizes on chosen branch family. */
    BRANCH_STABILIZING,
    /** Branch-specific features strengthen. */
    SPECIALIZATION_EMERGING,
    /** Lineage-specific form established for this device. */
    SPECIALIZATION_ESTABLISHED,
}
