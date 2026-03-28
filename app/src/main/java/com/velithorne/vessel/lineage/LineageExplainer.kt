package com.velithorne.vessel.lineage

import com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage

object LineageExplainer {

    fun stageTransition(
        from: SeedPodGrowthStage,
        to: SeedPodGrowthStage,
        channel: String,
    ): String =
        "Stage ${from.name.replace('_', ' ').lowercase()} → ${to.name.replace('_', ' ').lowercase()} ($channel)."

    fun crownVisible(driver: String): String =
        "Crown bud intensified under $driver."

    fun lateralExpanded(driver: String): String =
        "Lateral signal buds expanded under $driver."

    fun reserveContracted(): String =
        "Reserve bulb contracted during energy deficit."

    fun reserveExpanded(): String =
        "Reserve bulb swelled as reserves stabilized."

    fun shellThickened(thermal: Boolean): String =
        if (thermal) "Shell edge thickened under thermal strain."
        else "Shell edge thickened under structural load."

    fun chamberEnvelope(): String =
        "Chamber envelope broadened while inactive."

    fun coherenceImproved(): String =
        "Shell coherence improved during recovery."

    fun adaptationLabel(kind: AdaptationKind): String = when (kind) {
        AdaptationKind.THERMAL -> "Thermal strain"
        AdaptationKind.SIGNAL -> "Signal pressure"
        AdaptationKind.NEURAL -> "Neural load"
        AdaptationKind.RECOVERY -> "Recovery"
        AdaptationKind.RESERVE -> "Reserve stress"
        AdaptationKind.ARCHIVE -> "Archive burden"
    }
}
