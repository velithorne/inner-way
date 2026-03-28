package com.velithorne.vessel.growth_seedpod

import com.velithorne.vessel.physiology.PhysiologySnapshot

/** Short deterministic lines for the Vessel seed-pod UI. */
object SeedPodExplainer {

    /** Must match [com.velithorne.vessel.renderer_seedpod.SeedPodTuning] bud thresholds — only mention what we draw. */
    private const val CROWN_MENTION = 0.22f
    private const val LATERAL_MENTION = 0.2f
    private const val RESERVE_MENTION = 0.22f
    private const val THERMAL_MENTION = 0.12f
    private const val HAZE_MENTION = 0.18f

    fun stageLabel(stage: SeedPodGrowthStage): String = when (stage) {
        SeedPodGrowthStage.DORMANT_POD -> "Dormant pod"
        SeedPodGrowthStage.ACTIVATING_POD -> "Activating pod"
        SeedPodGrowthStage.GERMINATING_POD -> "Germinating pod"
        SeedPodGrowthStage.EARLY_BUDDING -> "Early budding"
        SeedPodGrowthStage.EARLY_CHAMBERING -> "Early chambering"
        SeedPodGrowthStage.CHAMBER_MATURED -> "Chamber matured"
        SeedPodGrowthStage.LINEAGE_DIFFERENTIATING -> "Lineage differentiating"
        SeedPodGrowthStage.FIRST_BRANCH_FORMING -> "First branch forming"
        SeedPodGrowthStage.BRANCH_STABILIZING -> "Branch stabilizing"
        SeedPodGrowthStage.SPECIALIZATION_EMERGING -> "Specialization emerging"
        SeedPodGrowthStage.SPECIALIZATION_ESTABLISHED -> "Specialization established"
    }

    fun statusLine(phys: PhysiologySnapshot, state: SeedPodGrowthState): String {
        val s = phys.species
        val d = state.display
        val parts = mutableListOf<String>()
        if (d.crownNub > CROWN_MENTION) parts += "Crown bud responding to neural load."
        if (d.lateralBudLeft > LATERAL_MENTION || d.lateralBudRight > LATERAL_MENTION) {
            parts += "Lateral buds trace signal pressure."
        }
        if (d.reserveBulb > RESERVE_MENTION) parts += "Reserve bulb tracks energy state."
        if (d.thermalVeil > THERMAL_MENTION) parts += "Thermal veil brightens at the shell."
        if (d.tissueHaze > HAZE_MENTION) parts += "Local tissue haze gathers around the pod."
        if (parts.isEmpty()) {
            parts += if (s.vitality > 0.5f) "Pod stable; local growth within envelope." else "Pod adapting to chamber conditions."
        }
        return parts.take(2).joinToString(" ")
    }
}
