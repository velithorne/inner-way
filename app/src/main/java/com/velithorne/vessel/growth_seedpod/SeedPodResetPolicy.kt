package com.velithorne.vessel.growth_seedpod

/**
 * Specimen lifecycle reset is implemented in [com.velithorne.vessel.growthtime.GrowthResetPolicy],
 * driven by [com.velithorne.vessel.growthtime.TimeTuning.buildResetEnabled] from the active
 * [com.velithorne.vessel.config.GrowthProfile] (dev: wipe on version change; release: preserve lineage).
 */
object SeedPodResetPolicy
