package com.velithorne.vessel.growth_seedpod

import com.velithorne.vessel.physiology.PhysiologySnapshot

/** Short deterministic lines for the Vessel seed-pod UI. */
object SeedPodExplainer {

    fun stageLabel(stage: SeedPodGrowthStage): String = when (stage) {
        SeedPodGrowthStage.DORMANT_POD -> "Dormant pod"
        SeedPodGrowthStage.ACTIVATING_POD -> "Activating pod"
        SeedPodGrowthStage.GERMINATING_POD -> "Germinating pod"
        SeedPodGrowthStage.EARLY_BUDDING -> "Early budding"
        SeedPodGrowthStage.EARLY_CHAMBERING -> "Early chambering"
    }

    fun statusLine(phys: PhysiologySnapshot, state: SeedPodGrowthState): String {
        val s = phys.species
        val d = state.display
        val parts = mutableListOf<String>()
        if (d.crownNub > 0.35f) parts += "Crown bud responding to neural load."
        if (d.lateralBudLeft + d.lateralBudRight > 0.5f) parts += "Lateral buds trace signal pressure."
        if (d.reserveBulb > 0.4f) parts += "Reserve bulb tracks energy state."
        if (d.thermalVeil > 0.35f) parts += "Thermal veil brightens at the shell."
        if (d.tissueHaze > 0.3f) parts += "Local tissue haze gathers around the pod."
        if (parts.isEmpty()) {
            parts += if (s.vitality > 0.5f) "Pod stable; local growth within envelope." else "Pod adapting to chamber conditions."
        }
        return parts.take(2).joinToString(" ")
    }
}
