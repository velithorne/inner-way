package com.velithorne.vessel.morphogenesis_core

import com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage

object EraMapper {

    fun fromStage(stage: SeedPodGrowthStage): CanonicalLifeEra = when (stage) {
        SeedPodGrowthStage.DORMANT_POD,
        SeedPodGrowthStage.ACTIVATING_POD,
        -> CanonicalLifeEra.SEED
        SeedPodGrowthStage.GERMINATING_POD,
        SeedPodGrowthStage.EARLY_BUDDING,
        -> CanonicalLifeEra.VEIL_STAGE
        SeedPodGrowthStage.EARLY_CHAMBERING,
        SeedPodGrowthStage.CHAMBER_MATURED,
        -> CanonicalLifeEra.CORE_ESTABLISHMENT
        SeedPodGrowthStage.LINEAGE_DIFFERENTIATING,
        SeedPodGrowthStage.FIRST_BRANCH_FORMING,
        -> CanonicalLifeEra.BRANCHING_THRESHOLD
        SeedPodGrowthStage.BRANCH_STABILIZING,
        SeedPodGrowthStage.SPECIALIZATION_EMERGING,
        SeedPodGrowthStage.SPECIALIZATION_ESTABLISHED,
        -> CanonicalLifeEra.ADULTHOOD
    }
}
