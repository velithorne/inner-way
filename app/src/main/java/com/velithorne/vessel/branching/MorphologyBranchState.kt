package com.velithorne.vessel.branching

/**
 * Persisted branching — affinities, readiness, soft lead (ordinal), commitment level.
 */
data class MorphologyBranchState(
    val affinities: BranchAffinity,
    /** 0..1 — when lineage differentiation is meaningful. */
    val branchReadiness: Float,
    /** Ordinal of [LineageBranch] — leading tendency (soft commit). */
    val leadingBranchOrdinal: Int,
    /** 0 = none, 1 = leaning, 2 = forming, 3 = established. */
    val commitmentLevel: Int,
) {
    companion object {
        fun initial() = MorphologyBranchState(
            affinities = BranchAffinity.uniform(),
            branchReadiness = 0f,
            leadingBranchOrdinal = LineageBranch.BALANCED.ordinal,
            commitmentLevel = 0,
        )
    }

    fun leadingBranch(): LineageBranch =
        LineageBranch.entries.getOrNull(leadingBranchOrdinal) ?: LineageBranch.BALANCED
}
