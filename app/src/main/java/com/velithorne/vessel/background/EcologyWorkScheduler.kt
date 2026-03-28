package com.velithorne.vessel.background

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.velithorne.vessel.BuildConfig
import java.util.concurrent.TimeUnit

const val ECOLOGY_PERIODIC_WORK_NAME = "velithorne_ecology_periodic"

object EcologyWorkScheduler {

    fun schedule(context: Context, tuning: BackgroundTuning) {
        val intervalMs = if (BuildConfig.DEBUG) tuning.devPeriodicWorkIntervalMs else tuning.periodicWorkIntervalMs
        val intervalMin = TimeUnit.MILLISECONDS.toMinutes(intervalMs).coerceAtLeast(15L)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .build()
        val req = PeriodicWorkRequestBuilder<EcologyWorker>(intervalMin, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            ECOLOGY_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            req,
        )
    }
}
