package com.collide.app.domain.model

data class RunSummary(
    val id: Long = 0,
    val timestamp: Long,
    val fileName: String,
    val inputSize: Long,
    val runMode: RunMode,
    val stats: RunStats
)
