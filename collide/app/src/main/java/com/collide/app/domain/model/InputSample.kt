package com.collide.app.domain.model

/**
 * Represents a code or text input to the collision engine.
 * Content is held as raw text; profiling and normalization happen downstream.
 */
data class InputSample(
    val id: String,
    val label: String,
    val rawText: String,
    val sourceType: InputSourceType = InputSourceType.PASTE
) {
    val rawLength: Int get() = rawText.length

    companion object {
        const val MAX_INPUT_SIZE_BYTES = 50_000
    }
}

enum class InputSourceType {
    PASTE,
    FILE_IMPORT,
    FIXTURE
}
