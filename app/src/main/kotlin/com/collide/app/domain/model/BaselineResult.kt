package com.collide.app.domain.model

enum class BaselineStrategy(val displayName: String) {
    RAW_DEFLATE("Raw Deflate"),
    DEFLATE_BEST_COMPRESSION("Deflate Best Compression")
}

data class BaselineResult(
    val strategy: BaselineStrategy,
    val encodedSize: Long,
    val elapsedMs: Long
)
