package com.velithorne.vessel.background

import com.velithorne.vessel.data.LineageRepository

/**
 * Thin read API over Room ecology rows — keeps UI/debug off DAOs directly.
 */
class EcologySnapshotStore(
    private val lineageRepository: LineageRepository,
) {
    suspend fun recent(specimenId: String, limit: Int = 12) =
        lineageRepository.recentEcologySnapshots(specimenId, limit)
}
