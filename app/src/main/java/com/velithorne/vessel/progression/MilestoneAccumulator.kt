package com.velithorne.vessel.progression

/** Bit-flag milestone helpers — see [MilestoneBits]. */
object MilestoneAccumulator {
    fun merge(flags: Long, milestones: List<DevelopmentMilestone>): Long {
        var f = flags
        for (m in milestones) f = MilestoneBits.with(f, m)
        return f
    }
}
