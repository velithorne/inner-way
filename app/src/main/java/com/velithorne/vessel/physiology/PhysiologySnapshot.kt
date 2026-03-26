package com.velithorne.vessel.physiology

import com.velithorne.vessel.telemetry.TelemetrySnapshot

/**
 * Combined readout: raw telemetry plus derived biology.
 * Primary surface for Phase 3 renderer bindings and eventual Room snapshots.
 */
data class PhysiologySnapshot(
    val timestampMillis: Long,
    val telemetry: TelemetrySnapshot,
    val species: SpeciesState,
    val organs: List<OrganState>,
)
