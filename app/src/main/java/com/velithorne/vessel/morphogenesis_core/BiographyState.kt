package com.velithorne.vessel.morphogenesis_core

data class BiographyState(
    val scars: List<ScarPlate>,
    val thresholdFlags: Set<ThresholdEventKind>,
    val rerouteCount: Int,
    val moltCount: Int,
)
