package com.velithorne.vessel.model

import com.velithorne.vessel.genesis.MinimumViableBody

/** UI-facing MVB snapshot. */
data class MinimumViableBodyState(
    val kind: MinimumViableBody,
    val label: String,
)
