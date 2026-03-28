package com.velithorne.vessel.model

import com.velithorne.vessel.growthtime.GrowthSessionSummary
import com.velithorne.vessel.morphogenesis.MorphogenesisSnapshot

data class GrowthVisualState(
    val snapshot: MorphogenesisSnapshot,
    val statusLabel: String,
    val statusLine: String,
    val visibleActivity: Float,
    val germinationStageLabel: String,
    /** 0..1 lagging developmental progress (temporal growth). */
    val growthProgressFraction: Float,
    /** Strongest developmental budget channel for compact UI. */
    val activeBudgetChannelLabel: String,
    /** First line from last return summary while still shown; empty otherwise. */
    val recentAwayLine: String,
    /** Non-null after background if structural change detected. */
    val returnSummary: GrowthSessionSummary?,
)
