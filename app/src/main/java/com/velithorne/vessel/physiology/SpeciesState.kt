package com.velithorne.vessel.physiology

/**
 * Whole-organism synthetic condition derived from telemetry (Phase 2).
 * All metrics are normalized 0..1 unless noted.
 *
 * Future: persistent traits and mutation deltas apply on top of this real-time layer (Phase 3+).
 */
data class SpeciesState(
    val vitality: Float,
    val stress: Float,
    val hunger: Float,
    val fever: Float,
    val recovery: Float,
    val respiration: Float,
    val neuralActivity: Float,
    val mobility: Float,
    val sleepPressure: Float,
    val signalArousal: Float,
    val structuralLoad: Float,
    val healthLabel: String,
    val stateSummary: String,
)
