package com.velithorne.vessel.progression

/**
 * Irreversible structural unlocks — persisted as bit flags.
 */
enum class DevelopmentMilestone(val bit: Int) {
    CROWN_BUD(0),
    LATERAL_BUDS(1),
    RESERVE_BULB(2),
    SHELL_BAND(3),
    CHAMBER_ENVELOPE(4),
    CHAMBER_MATURED(5),
    LINEAGE_DIFFERENTIATION(6),
    FIRST_BRANCH_FORM(7),
    ADAPTIVE_SHELL(8),
    SPECIALIZATION_READY(9),
}

object MilestoneBits {
    fun has(flags: Long, m: DevelopmentMilestone): Boolean = (flags and (1L shl m.bit)) != 0L
    fun with(flags: Long, m: DevelopmentMilestone): Long = flags or (1L shl m.bit)
}
