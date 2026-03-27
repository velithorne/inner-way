package com.velithorne.vessel.morphogenesis_core

enum class MorphNodeKind {
    CORE_KNOT,
    RESERVE_BASIN_LOCUS,
    CROWN_CHAMBER,
    ARCHIVE_PLATE_SEED,
    FROND_ROOT,
    BRACE_ANCHOR,
    HEAT_MANTLE_RIDGE,
    TRANSIENT_GROWTH_CENTER,
    SCAR_ANCHOR,
    REROUTE_JUNCTION,
}

data class StructuralNode(
    val id: String,
    val kind: MorphNodeKind,
    /** Ontology class tag — must map to [SpeciesOntology] strings. */
    val ontologyClass: String,
    /** Normalized 0..1 body space */
    val nx: Float,
    val ny: Float,
    val strength: Float,
    val active: Boolean,
)
