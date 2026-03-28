package com.velithorne.vessel.juvenile_form

/**
 * Region weights 0..1 — which body architecture dominates this juvenile.
 */
data class JuvenileBodyPlan(
    val crownMass: Float,
    val coreMass: Float,
    val lateralMass: Float,
    val reserveMass: Float,
    val shellMass: Float,
    val supportMass: Float,
    val archiveMass: Float,
    val dominantRegion: JuvenileRegion,
)
