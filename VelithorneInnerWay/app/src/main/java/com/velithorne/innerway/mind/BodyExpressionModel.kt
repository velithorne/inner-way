package com.velithorne.innerway.mind

/**
 * Physiological expression parameters for the living seed (Phase 2).
 * All values are 0..1 unless noted; they modulate the existing pulse/breath renderer.
 */
data class BodyExpressionModel(
    /** Relative breath cadence; higher = faster respiratory cycle (maps to animation duration). */
    val breathRate: Float = 0.4f,
    /** Amplitude of the breathing undulation (depth of the living signal). */
    val breathDepth: Float = 0.55f,
    /** Strength of the luminous pulse / ring emphasis. */
    val pulseIntensity: Float = 0.55f,
    /** Overall luminance multiplier. */
    val brightness: Float = 0.75f,
    /** Tissue compression (works with law contraction). */
    val contraction: Float = 0.35f,
    /** Shimmer / edge instability (heat, stress). */
    val instability: Float = 0.15f,
    /** Expansion vs compression of form (inverse of defensive closure). */
    val openness: Float = 0.65f,
    /** Dampens micro-noise and deepens dormancy visuals (rest, dormancy). */
    val sleepDepth: Float = 0.2f,
)
