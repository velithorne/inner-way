package com.collide.app.domain.model

/**
 * Formal size accounting for a single candidate evaluation.
 *
 * Every byte required to reconstruct the original file is counted.
 * totalEncodedSize is the definitive number used for winner classification.
 */
data class SizeBreakdown(
    /** Bytes after all transforms have been applied (before backend compression). */
    val transformedPayloadSize: Long,
    /** Bytes after backend compression of the transformed payload. */
    val backendCompressedPayloadSize: Long,
    /** Total bytes consumed by all per-transform metadata blobs (including their 4-byte length prefixes). */
    val transformMetadataSize: Long,
    /** Fixed container header: 4 bytes num_transforms + 4 bytes original_size. */
    val containerHeaderSize: Long = 8L,
    /** Original uncompressed input size (informational). */
    val originalInputSize: Long
) {
    /** The canonical total: every byte needed for decoding. */
    val totalEncodedSize: Long
        get() = backendCompressedPayloadSize + transformMetadataSize + containerHeaderSize

    /** How many bytes the transform chain alone adds beyond the compressed payload. */
    val overheadSize: Long
        get() = transformMetadataSize + containerHeaderSize

    fun toDisplayMap(): Map<String, String> = linkedMapOf(
        "Original Input" to formatBytes(originalInputSize),
        "Transformed Payload" to formatBytes(transformedPayloadSize),
        "Backend Compressed" to formatBytes(backendCompressedPayloadSize),
        "Transform Metadata" to formatBytes(transformMetadataSize),
        "Container Header" to formatBytes(containerHeaderSize),
        "Total Encoded" to formatBytes(totalEncodedSize),
        "Overhead" to formatBytes(overheadSize)
    )

    private fun formatBytes(b: Long): String = when {
        b < 1024 -> "$b B"
        b < 1024 * 1024 -> "%.1f KB".format(b / 1024.0)
        else -> "%.2f MB".format(b / (1024.0 * 1024.0))
    }
}
