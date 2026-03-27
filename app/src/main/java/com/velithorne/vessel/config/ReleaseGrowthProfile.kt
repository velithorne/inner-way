package com.velithorne.vessel.config

import com.velithorne.vessel.background.BackgroundTuning
import com.velithorne.vessel.branching.BranchingTuning
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthTuning
import com.velithorne.vessel.growthtime.TimeTuning
import com.velithorne.vessel.progression.ProgressionTuning
import java.util.concurrent.TimeUnit

/**
 * Long-term companion pacing — lineage persists across app updates ([TimeTuning.buildResetEnabled] = false).
 */
object ReleaseGrowthProfile {

    fun build(): GrowthProfile {
        val prog = ProgressionTuning()
        val branch = BranchingTuning()
        val bg = BackgroundTuning(
            periodicWorkIntervalMs = TimeUnit.HOURS.toMillis(4),
            devPeriodicWorkIntervalMs = TimeUnit.MINUTES.toMillis(15),
        )
        val time = TimeTuning(
            buildResetEnabled = false,
        )
        val seed = SeedPodGrowthTuning.default()
        return GrowthProfile(
            mode = SimulationMode.RELEASE_REALTIME,
            progression = prog,
            branching = branch,
            background = bg,
            time = time,
            seedPodGrowth = seed,
            seedPodMaxOfflineCatchUpMs = 120_000L,
        )
    }
}
