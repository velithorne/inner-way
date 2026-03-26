package com.velithorne.vessel.core

object Constants {
    const val LOG_TAG = "VelithorneVessel"

    /** Wall-clock + sensor fusion refresh for debug UI. */
    const val TELEMETRY_TICK_MS = 500L

    /** EMA alpha for motion intensity (0–1); higher = faster response. */
    const val MOTION_SMOOTHING_ALPHA = 0.25f

    /**
     * Future modules (non-exhaustive):
     * - PhysiologyEngine: maps normalized telemetry → organ stress / homeostasis
     * - SpeciesState / OrganState: persisted phenotype snapshots
     * - EvolutionEngine: long-horizon mutation / selection
     * - Room: telemetry traces, seeds, save slots
     * - VesselRenderer: 2.5D / creature presentation layer
     */
    object FuturePhases {
        const val PHYSIOLOGY_MODULE = "com.velithorne.vessel.domain.physiology"
        const val EVOLUTION_MODULE = "com.velithorne.vessel.domain.evolution"
        const val PERSISTENCE_MODULE = "com.velithorne.vessel.data.persistence"
        const val RENDERER_MODULE = "com.velithorne.vessel.ui.vessel"
    }
}
