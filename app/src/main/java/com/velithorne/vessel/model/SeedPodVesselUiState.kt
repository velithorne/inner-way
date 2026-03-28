package com.velithorne.vessel.model

import com.velithorne.vessel.growthtime.GrowthSessionSummary
import com.velithorne.vessel.lineage.SeedPodReturnSummary

/** Vessel tab UI — permanent structural stage vs live condition vs progress semantics. */
data class SeedPodVesselUiState(
    /** Irreversible developmental stage label. */
    val structuralStageLabel: String,
    /** Reversible live presentation (Calm / Strained / …). */
    val liveConditionLabel: String,
    val statusLine: String,
    /** Monotonic structural advancement 0..1 (smoothed). */
    val growthProgressFraction: Float,
    val progressCaption: String,
    val nextStageLabel: String,
    /** Optional secondary indicator 0..1 — strain, not development loss. */
    val liveStrainIndicator: Float,
    val activeBudgetChannelLabel: String,
    val recentAwayLine: String,
    val returnSummary: GrowthSessionSummary?,
    val seedPodReturnSummary: SeedPodReturnSummary?,
    /** Shown after lineage differentiation — morphology family leaning. */
    val branchStatusLine: String,
    val branchReasonLine: String,
    /** One line about coarse background ecology sampling (WorkManager). */
    val ambientEcologyHintLine: String,
    /** Non-null in debug when [com.velithorne.vessel.config.SimulationMode.DEV_SIMULATION] is active. */
    val devSimulationHintLine: String? = null,
    /** Debug dev mode: persisted evolution speed multiplier (1 = default dev pacing). Null in release. */
    val devEvolutionSpeedMultiplier: Float? = null,
    /** Lines describing visible topology — must align with canvas. */
    val visibleTopologyLines: List<String> = emptyList(),
    /** Short line: dominant contour driver from self-assembly. */
    val morphologyDriverLine: String? = null,
    /** Debug-only: influence split + asymmetry (default null). */
    val visibilityDebugLine: String? = null,
    /** Juvenile body architecture phase — empty before juvenile emergence. */
    val juvenileFormLines: List<String> = emptyList(),
    /** Short labels for trait row (crown, lateral, etc.). */
    val juvenileTraitChips: List<String> = emptyList(),
    /** One line for topology expansion chip. */
    val juvenileTopologyStageLine: String? = null,
    /** Seed genesis / minimum viable body — empty after early growth. */
    val genesisSummaryLines: List<String> = emptyList(),
    val genesisDriverLine: String? = null,
    val birthStateChipLabel: String? = null,
)
