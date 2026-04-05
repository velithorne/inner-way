package com.velithorne.innerway.services

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.velithorne.innerway.memory.MemoryDatabase
import com.velithorne.innerway.memory.MemoryRepository
import com.velithorne.innerway.memory.ReflectionEngine
import java.util.concurrent.TimeUnit

/**
 * Low-frequency background rhythm. Does not hammer sensors; occasional reflection only.
 */
class SpeciesCycleWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repo = MemoryRepository(MemoryDatabase.get(applicationContext).memoryDao())
        ReflectionEngine(repo).maybeReflect()
        return Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "velithorne_species_cycle"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<SpeciesCycleWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
