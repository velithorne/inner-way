package com.velithorne.innerway.body

import android.os.SystemClock
import java.util.Calendar

/**
 * Circadian pulse from clock and coarse uptime (screen state wired in Phase 2).
 */
class CircadianRhythmSystem {

    data class Sample(
        val circadianPhase: Float,
        val screenLikelyAwake: Boolean,
        val timestampMillis: Long,
    )

    suspend fun sample(): Sample {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60f
        val phase = ((hour % 24f) / 24f).coerceIn(0f, 1f)
        val night = hour < 6f || hour > 22f
        val uptimeHours = SystemClock.uptimeMillis() / 3_600_000f
        val awake = !night || uptimeHours < 0.25f
        return Sample(
            circadianPhase = phase,
            screenLikelyAwake = awake,
            timestampMillis = System.currentTimeMillis(),
        )
    }
}
