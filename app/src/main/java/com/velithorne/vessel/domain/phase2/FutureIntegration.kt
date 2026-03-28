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
 * - **VesselRenderer** + **MorphogenesisEngine**: growth graph + procedural anatomy in `com.velithorne.vessel`; evolution overlays later
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
