package com.velithorne.vessel.growthtime

import com.velithorne.vessel.data.prefs.AppBuildStateStore
import com.velithorne.vessel.data.prefs.GrowthStateStore
import com.velithorne.vessel.morphogenesis.MorphogenesisEngine

object GrowthResetPolicy {

    /**
     * If [currentVersionCode] != stored, clear growth prefs and reset morphogenesis.
     */
    fun applyIfNewBuild(
        currentVersionCode: Int,
        buildStore: AppBuildStateStore,
        growthStore: GrowthStateStore,
        morphogenesisEngine: MorphogenesisEngine,
        tuning: TimeTuning,
    ): Boolean {
        if (!tuning.buildResetEnabled) return false
        val last = buildStore.getLastVersionCode()
        if (last == currentVersionCode) return false
        growthStore.clear()
        morphogenesisEngine.resetForNewBuild()
        buildStore.setLastVersionCode(currentVersionCode)
        return true
    }
}
