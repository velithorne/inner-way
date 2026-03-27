package com.velithorne.vessel.config

/**
 * High-level growth pacing — one profile for dev iteration, one for shipped users.
 */
enum class SimulationMode {
    /** Fast iteration: fresh specimen per dev build, accelerated pacing. */
    DEV_SIMULATION,

    /** Long-term companion: preserve lineage across updates, conservative pacing. */
    RELEASE_REALTIME,
}
