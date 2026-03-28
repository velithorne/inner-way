package com.velithorne.vessel.genesis

/**
 * Immutable snapshot for one coordinator step (ties traits + classification + copy).
 */
data class SeedGenesisSnapshot(
    val state: SeedGenesisState,
)
