package com.velithorne.vessel.model

import com.velithorne.vessel.morphogenesis.MorphogenesisSnapshot

data class GrowthVisualState(
    val snapshot: MorphogenesisSnapshot,
    val statusLabel: String,
    val statusLine: String,
    val visibleActivity: Float,
    val germinationStageLabel: String,
)
