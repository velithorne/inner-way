package com.velithorne.vessel.background

import java.util.concurrent.TimeUnit

/**
 * Battery-conscious ambient sampling — single place for intervals and caps.
 */
data class BackgroundTuning(
    /** WorkManager periodic interval (production). Android may stretch under Doze. */
    val periodicWorkIntervalMs: Long = TimeUnit.HOURS.toMillis(4),
    /** Optional shorter interval in debug builds ([BuildConfig.DEBUG]). */
    val devPeriodicWorkIntervalMs: Long = TimeUnit.MINUTES.toMillis(15),
    /** Minimum gap between snapshot writes per specimen (debounce). */
    val snapshotWriteDebounceMs: Long = 60_000L,
    /** Max ecology rows to keep per specimen before pruning oldest. */
    val maxEcologySnapshotsPerSpecimen: Int = 200,
    /** Max ambient event rows per specimen. */
    val maxAmbientEventsPerSpecimen: Int = 120,
    /** Max simulated seconds of growth applied per reopen from ambient backlog. */
    val maxAmbientCatchUpSimulatedSec: Float = 90f,
    /** Chunk size for stepping growth during ambient catch-up (seconds). */
    val ambientCatchUpStepSec: Float = 3f,
    /** Minimum away time (ms) before ambient reopen processing runs. */
    val minAwayMsForAmbientProcess: Long = 5_000L,
    /** Threshold for "many charging samples" in summaries. */
    val chargingSampleStrongThreshold: Int = 3,
    /** Threshold for mobile-heavy ecology in summaries. */
    val cellularSampleStrongThreshold: Int = 4,
    /** Thermal stress: battery temp above this (C) counts as warm. */
    val warmBatteryTempC: Float = 36f,
    val hotBatteryTempC: Float = 40f,
)
