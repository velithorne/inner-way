package com.velithorne.vessel.model

/**
 * Flags for the active Vessel render path — genesis birth vs legacy scaffold (must stay false in genesis era).
 */
data class GenesisVisualState(
    val genesisRenderPathActive: Boolean,
    val legacySeedScaffoldSuppressed: Boolean,
    val minimumViableBodyLabel: String,
    val genesisContourDriver: String,
)
