package com.velithorne.vessel.morphogenesis_core

/**
 * Historical pressure channels — long EMA memory in [GrowthPressureState].
 */
enum class PressureChannel {
    THERMAL,
    RESERVE,
    STARVATION,
    RECOVERY,
    SIGNAL,
    ARCHIVE,
    MOTION,
    CIRCADIAN,
    ISOLATION,
    COHERENCE,
    MUTATION,
}
