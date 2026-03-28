package com.velithorne.vessel.background

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager

/** Read-only WorkManager state for UI/debug — does not schedule work. */
object AmbientWorkStatus {

    fun isPeriodicEcologyScheduled(context: Context): Boolean {
        val infos = try {
            WorkManager.getInstance(context.applicationContext)
                .getWorkInfosForUniqueWork(ECOLOGY_PERIODIC_WORK_NAME)
                .get()
        } catch (_: Exception) {
            emptyList()
        }
        return infos.any {
            it.state == WorkInfo.State.ENQUEUED ||
                it.state == WorkInfo.State.RUNNING ||
                it.state == WorkInfo.State.BLOCKED
        }
    }
}
