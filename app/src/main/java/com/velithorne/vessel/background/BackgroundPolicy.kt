package com.velithorne.vessel.background

import android.content.Context
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.velithorne.vessel.BuildConfig

/**
 * **Battery-safe background rules**
 *
 * - **Sampled in background (WorkManager / passive):** battery %, charging, battery temp, power save,
 *   storage %, low memory flag, network connected/type/metered, uptime, screen interactive, coarse motion
 *   (often null without foreground sensors — we still record ecology).
 * - **Deferred to foreground:** full accelerometer smoothing, organ hit-testing, live frame simulation.
 * - **Why safe:** one short WorkManager job on a coarse schedule; no sensor wake loop; bounded Room writes;
 *   idempotent work; if WorkManager is deferred, [com.velithorne.vessel.growth_seedpod.SeedPodGrowthCoordinator]
 *   still applies bounded catch-up on reopen using stored snapshots.
 */
object BackgroundPolicy {

    fun shouldRunPeriodicWork(context: Context): Boolean {
        if (BuildConfig.DEBUG) return true
        val pm = ContextCompat.getSystemService(context, PowerManager::class.java) ?: return true
        return !pm.isPowerSaveMode
    }

    fun shouldWriteSnapshot(nowMs: Long, lastWriteMs: Long, tuning: BackgroundTuning): Boolean =
        nowMs - lastWriteMs >= tuning.snapshotWriteDebounceMs
}
