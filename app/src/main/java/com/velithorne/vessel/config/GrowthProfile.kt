package com.velithorne.vessel.config

import com.velithorne.vessel.background.BackgroundTuning
import com.velithorne.vessel.branching.BranchingTuning
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthTuning
import com.velithorne.vessel.growthtime.TimeTuning
import com.velithorne.vessel.progression.ProgressionTuning

/**
 * Single bundle of tunings for one [SimulationMode] — engines read from here, not scattered if/else.
 */
data class GrowthProfile(
    val mode: SimulationMode,
    val progression: ProgressionTuning,
    val branching: BranchingTuning,
    val background: BackgroundTuning,
    val time: TimeTuning,
    val seedPodGrowth: SeedPodGrowthTuning,
    /** Max wall ms for seed-pod offline catch-up (Vessel tab) — separate from morphogenesis [TimeTuning]. */
    val seedPodMaxOfflineCatchUpMs: Long,
)
