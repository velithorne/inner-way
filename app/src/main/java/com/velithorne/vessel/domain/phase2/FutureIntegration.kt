package com.velithorne.vessel.domain.phase2

/**
 * Phase 2+ integration anchors (stubs only — do not wire yet).
 *
 * - PhysiologyEngine: map TelemetrySnapshot streams → metabolic / stress scalars per organ.
 * - SpeciesState / OrganState: identity + phenotype for evolution + renderer binding.
 * - EvolutionEngine: generational pressure from long-window telemetry and survival proxies.
 * - VesselPersistence: Room entities / DAOs for snapshots, species seeds, replay.
 * - VesselRenderer: 2.5D presentation; read-only consumer of SpeciesState.
 */
interface PhysiologyEngine {
    // TODO: fun ingest(snapshot: com.velithorne.vessel.telemetry.TelemetrySnapshot): PhysiologyDelta
}

data class SpeciesState(
    val placeholder: String = "phase2",
)

data class OrganState(
    val placeholder: String = "phase2",
)

interface EvolutionEngine {
    // TODO: fun observeFitness(): Flow<FitnessScalar>
}

interface VesselPersistence {
    // TODO: Room database holder + DAOs
}

interface VesselRenderer {
    // TODO: bind rendering to latest vessel / species projection
}
