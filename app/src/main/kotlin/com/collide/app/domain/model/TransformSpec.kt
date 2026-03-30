package com.collide.app.domain.model

data class TransformSpec(
    val id: String,
    val displayName: String,
    val estimatedCostMs: Int = 1
)
