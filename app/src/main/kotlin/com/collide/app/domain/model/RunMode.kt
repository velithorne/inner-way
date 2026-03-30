package com.collide.app.domain.model

enum class RunMode(
    val displayName: String,
    val maxCandidates: Int,
    val maxChainLength: Int,
    val pruneFactor: Float
) {
    SAFE("Safe", maxCandidates = 30, maxChainLength = 2, pruneFactor = 0.5f),
    BALANCED("Balanced", maxCandidates = 80, maxChainLength = 3, pruneFactor = 0.3f),
    BURST("Burst", maxCandidates = 200, maxChainLength = 3, pruneFactor = 0.1f)
}
