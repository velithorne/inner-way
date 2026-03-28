package com.velithorne.vessel.lineage

/**
 * Persistent identity for the single active specimen in this install lineage.
 */
data class SpecimenIdentity(
    val specimenId: String,
    val displayLabel: String,
    val seedType: String,
    val creationTimestampMillis: Long,
    val lastUpdateTimestampMillis: Long,
    val buildVersionCreatedOn: Int,
    val lineageGeneration: Int,
    val active: Boolean,
)
