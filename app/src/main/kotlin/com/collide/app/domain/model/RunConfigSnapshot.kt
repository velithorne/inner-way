package com.collide.app.domain.model

/**
 * Complete snapshot of the configuration used for a run.
 * Stored with every winning event so the exact run can be reproduced.
 */
data class RunConfigSnapshot(
    val runMode: String,
    val baselineStrategy: String,
    val maxCandidates: Int,
    val maxChainLength: Int,
    val enabledTransformIds: List<String>,
    val engineVersion: String = ENGINE_VERSION
) {
    companion object {
        const val ENGINE_VERSION = "2.0.0-phase2"
    }
}
