package com.velithorne.vessel.branching

/**
 * Normalized 0..1 affinity per branch — persisted as FloatArray in [MorphologyBranchState].
 */
data class BranchAffinity(
    val thermalShell: Float,
    val signalFrond: Float,
    val crownNeural: Float,
    val reserveBasin: Float,
    val archiveCore: Float,
    val motionBraced: Float,
    val balanced: Float,
) {
    fun asArray(): FloatArray = floatArrayOf(
        thermalShell, signalFrond, crownNeural, reserveBasin, archiveCore, motionBraced, balanced,
    )

    fun normalized(): BranchAffinity {
        val sum = thermalShell + signalFrond + crownNeural + reserveBasin + archiveCore + motionBraced + balanced
        if (sum < 1e-4f) return uniform()
        val inv = 1f / sum
        return BranchAffinity(
            thermalShell * inv,
            signalFrond * inv,
            crownNeural * inv,
            reserveBasin * inv,
            archiveCore * inv,
            motionBraced * inv,
            balanced * inv,
        )
    }

    operator fun get(branch: LineageBranch): Float = when (branch) {
        LineageBranch.THERMAL_SHELL -> thermalShell
        LineageBranch.SIGNAL_FROND -> signalFrond
        LineageBranch.CROWN_NEURAL -> crownNeural
        LineageBranch.RESERVE_BASIN -> reserveBasin
        LineageBranch.ARCHIVE_CORE -> archiveCore
        LineageBranch.MOTION_BRACED -> motionBraced
        LineageBranch.BALANCED -> balanced
    }

    companion object {
        fun fromArray(a: FloatArray): BranchAffinity {
            val t = a.getOrElse(0) { 1f / 7f }
            val s = a.getOrElse(1) { 1f / 7f }
            val c = a.getOrElse(2) { 1f / 7f }
            val r = a.getOrElse(3) { 1f / 7f }
            val ar = a.getOrElse(4) { 1f / 7f }
            val m = a.getOrElse(5) { 1f / 7f }
            val b = a.getOrElse(6) { 1f / 7f }
            return BranchAffinity(t, s, c, r, ar, m, b).normalized()
        }

        fun uniform(): BranchAffinity =
            BranchAffinity(1f / 7f, 1f / 7f, 1f / 7f, 1f / 7f, 1f / 7f, 1f / 7f, 1f / 7f)
    }
}
