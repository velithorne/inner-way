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
)
