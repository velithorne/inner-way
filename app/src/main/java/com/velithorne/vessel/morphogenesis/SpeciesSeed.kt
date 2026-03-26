package com.velithorne.vessel.morphogenesis

/**
 * Deterministic lineage seed — same [lineageId] reproduces the same initial biases.
 */
data class SpeciesSeed(
    val lineageId: Long,
    val symmetryBias: Float,
    val shellBias: Float,
    val branchingBias: Float,
    val archiveBias: Float,
    val thermalToleranceBias: Float,
    val signalBias: Float,
    val reserveBias: Float,
    val musculatureBias: Float,
    val densityBias: Float,
    val coherenceBias: Float,
) {
    companion object {
        fun fromLineageId(id: Long): SpeciesSeed {
            var x = id xor 0x6A09E667F3BCC909L // split-mix constant (fits signed long)
            fun next(): Float {
                x = x * 6364136223843003003L + 1L
                return ((x ushr 33).toInt() and 0xFFFF) / 65535f
            }
            return SpeciesSeed(
                lineageId = id,
                symmetryBias = 0.55f + next() * 0.35f,
                shellBias = 0.4f + next() * 0.45f,
                branchingBias = 0.35f + next() * 0.5f,
                archiveBias = 0.38f + next() * 0.48f,
                thermalToleranceBias = 0.42f + next() * 0.4f,
                signalBias = 0.4f + next() * 0.45f,
                reserveBias = 0.45f + next() * 0.4f,
                musculatureBias = 0.4f + next() * 0.45f,
                densityBias = 0.35f + next() * 0.45f,
                coherenceBias = 0.5f + next() * 0.35f,
            )
        }
    }
}
