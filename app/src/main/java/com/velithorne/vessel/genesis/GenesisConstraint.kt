package com.velithorne.vessel.genesis

/**
 * Hard rules for valid Velithorne birth — no full juvenile body, no arbitrary noise shapes.
 */
object GenesisConstraint {
    const val NO_FULL_JUVENILE_AT_BIRTH: Boolean = true
    const val RESERVE_BELOW_CORE_BIAS: Boolean = true
    const val CROWN_UPWARD_BIAS: Boolean = true
    const val SHELL_ONLY_WHERE_COHERENCE: Boolean = true
}
