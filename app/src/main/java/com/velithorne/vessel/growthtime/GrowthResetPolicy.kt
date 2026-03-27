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
     */
    fun applyIfNewBuild(
        currentVersionCode: Int,
        buildStore: AppBuildStateStore,
        growthStore: GrowthStateStore,
        morphogenesisEngine: MorphogenesisEngine,
        lineageRepository: LineageRepository,
        tuning: TimeTuning,
    ): Boolean {
        if (!tuning.buildResetEnabled) return false
        val last = buildStore.getLastVersionCode()
        if (last == currentVersionCode) return false
        growthStore.clear()
        morphogenesisEngine.resetForNewBuild()
        runBlocking {
            lineageRepository.clearAllLineage()
        }
        buildStore.setLastVersionCode(currentVersionCode)
        return true
    }
}
