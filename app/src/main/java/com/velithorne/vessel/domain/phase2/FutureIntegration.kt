package com.velithorne.vessel.domain.phase2

/**
 * Phase 3+ integration anchors beyond real-time physiology.
 *
 * Implemented in **Phase 2** (live mapping):
 * - `com.velithorne.vessel.physiology.PhysiologyEngine`
 * - `SpeciesState`, `OrganState`, `PhysiologySnapshot`
 *
 * Still future:
 * - **EvolutionEngine**: generational pressure from long-window physiology integrals
 * - **VesselPersistence**: Room / DataStore for traces and seeds
 * - **VesselRenderer**: 2.5D organ field driven by [com.velithorne.vessel.physiology.PhysiologySnapshot]
 */
interface EvolutionEngine {
    // TODO: fun observeFitness(): Flow<FitnessScalar>
}

interface VesselPersistence {
    // TODO: Room database holder + DAOs
}

interface VesselRenderer {
    // TODO: bind rendering to latest vessel / species projection
}
