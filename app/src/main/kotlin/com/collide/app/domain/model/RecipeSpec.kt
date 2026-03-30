package com.collide.app.domain.model

enum class BackendCompressor(val displayName: String) {
    DEFLATE_DEFAULT("Deflate/Default"),
    DEFLATE_BEST_COMPRESSION("Deflate/BestCompression")
}

data class RecipeSpec(
    val id: String,
    val transformIds: List<String>,
    val backend: BackendCompressor
) {
    fun summary(): String {
        val chain = if (transformIds.isEmpty()) "identity" else transformIds.joinToString(" → ")
        return "$chain | ${backend.displayName}"
    }
}
