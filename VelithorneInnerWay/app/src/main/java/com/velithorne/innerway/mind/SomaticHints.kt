package com.velithorne.innerway.mind

/**
 * Derived body rhythm cues from continuous device sampling (stillness, motion spikes).
 * Feeds [InternalStateEngine] and [BodyExpressionMapper]; not raw sensor fakery.
 */
data class SomaticHints(
    /** Seconds of consecutive low motion — deepens rest expression when appropriate. */
    val stillnessDurationSeconds: Float = 0f,
    /** After motion spikes — briefly increases alert / curious expression. */
    val motionAlertSecondsRemaining: Float = 0f,
    /** Accumulated disturbance (0..1), decays over time — favors DEFENSIVE when high. */
    val disturbanceScore: Float = 0f,
)
