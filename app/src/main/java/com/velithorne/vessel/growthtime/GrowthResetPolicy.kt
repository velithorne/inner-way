package com.velithorne.vessel.growthtime

import com.velithorne.vessel.data.LineageRepository
import com.velithorne.vessel.data.prefs.AppBuildStateStore
import com.velithorne.vessel.data.prefs.GrowthStateStore
import com.velithorne.vessel.morphogenesis.MorphogenesisEngine
import kotlinx.coroutines.runBlocking

object GrowthResetPolicy {

/**
 * If [currentVersionCode] != stored, clear growth prefs, reset morphogenesis, and **wipe Room lineage**
 * ([LineageRepository.clearAllLineage]) so the next step can create a fresh specimen.
 *
 * **Dev vs release:** [TimeTuning.buildResetEnabled] is true only in [com.velithorne.vessel.config.DevSimulationProfile];
 * release keeps lineage across normal updates.
 */
    fun applyIfNewBuild(
        currentVersionCode: Int,
        buildStore: AppBuildStateStore,
        growthStore: GrowthStateStore,
        morphogenesisEngine: MorphogenesisEngine,
        lineageRepository: LineageRepository,
        tuning: TimeTuning,
    ): Boolean {
        val last = buildStore.getLastVersionCode()
        if (last == currentVersionCode) return false
        if (!tuning.buildResetEnabled) {
            // Release: persist version so we do not treat every install as "new", but keep lineage.
            buildStore.setLastVersionCode(currentVersionCode)
            return false
        }
        growthStore.clear()
        morphogenesisEngine.resetForNewBuild()
        runBlocking {
            lineageRepository.clearAllLineage()
        }
        buildStore.setLastVersionCode(currentVersionCode)
        return true
    }
}
