package com.velithorne.vessel.renderer

import androidx.compose.ui.graphics.Color

/**
 * Renderer-ready frame derived from [com.velithorne.vessel.physiology.PhysiologySnapshot].
 * No wall-clock animation here — combin with [VesselAnimationController] phase in UI.
 *
 * Persistence / evolution will snapshot this + seed ids in a later phase.
 */
data class VesselSceneState(
    val timestampMillis: Long,
    val bodyScale: Float,
    val bodyPulseAmplitude: Float,
    val bodyBreathAmplitude: Float,
    val vitalityGlow: Float,
    val stressTint: Float,
    val feverIntensity: Float,
    val recoveryGlow: Float,
    val mobilitySway: Float,
    val signalBrightness: Float,
    val sleepDimming: Float,
    val structuralMass: Float,
    val stressShiver: Float,
    val neuralDrive: Float,
    val respirationDrive: Float,
    val hungerDim: Float,
    val thermalAgitation: Float,
    val particleDensity: Float,
    val fogDensity: Float,
    val signalStrained: Float,
    val organVisuals: List<OrganVisualModel>,
    val vascularPulse: Float,
    val statusLine: String,
    val healthLabel: String,
    val vitalityDisplay: Float,
    val feverLabel: String,
    val hungerLabel: String,
    val accentBias: Color,
    val thermalTint: Color,
)
