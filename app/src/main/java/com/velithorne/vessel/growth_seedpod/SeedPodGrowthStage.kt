package com.velithorne.vessel.growth_seedpod

/**
 * Seed-pod-only developmental stages — no legacy organism scaffold.
 */
enum class SeedPodGrowthStage {
    DORMANT_POD,
    ACTIVATING_POD,
    GERMINATING_POD,
    EARLY_BUDDING,
    EARLY_CHAMBERING,
    /**
     * Final seed-pod envelope — reached after sustained refinement in early chambering.
     */
    CHAMBER_MATURED,
    /** Post-mature lineage path — structural identity diverges from saved adaptation history. */
    LINEAGE_DIFFERENTIATING,
    /** First persistent morphology family (placeholder visuals; progression persisted). */
    FIRST_BRANCH_FORMING,
    /** Shell specialization begins. */
    ADAPTIVE_SHELL_VARIANT,
    /** Ready for deeper branching phase — keep last for ordinal / Room. */
    SPECIALIZATION_READY,
}
