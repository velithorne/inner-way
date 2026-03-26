package com.velithorne.vessel.growthtime

/**
 * Developmental stage for temporal growth (dwell + gates), distinct from instant classifier.
 */
enum class TemporalGrowthStage {
    DORMANT_SEED,
    ACTIVATED_SEED,
    GERMINATING,
    CROWN_FORMING,
    LATERAL_BUDDING,
    CHAMBER_DEEPENING,
    SHELL_ACCRETING,
    STABILIZING,
}
