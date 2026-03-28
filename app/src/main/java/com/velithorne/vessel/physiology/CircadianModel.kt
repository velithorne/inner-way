package com.velithorne.vessel.physiology

import java.util.Calendar
import kotlin.math.cos

/**
 * Placeholder circadian prior from wall clock only (no sleep staging API).
 * Phase 3+ can swap in learned rhythms or fused sensors.
 */
object CircadianModel {

    /** 0 at ~14:00, peaks around typical sleep onset (night bias for pressure). */
    fun nightBias01(timestampMillis: Long, tuning: PhysiologyTuning): Float {
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60f
        val phase = ((hour - 6.0) / 24.0) * PhysiologyTuning.TAU
        val raw = ((1.0 - cos(phase)) / 2.0).toFloat()
        return (raw * tuning.circadianNightAmplitude).coerceIn(0f, 1f)
    }

    /** Monotonic phase 0..1 across the local day (for hooks / debug). */
    fun phase01(timestampMillis: Long): Float {
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60f
        return (hour / 24f).coerceIn(0f, 1f)
    }
}
