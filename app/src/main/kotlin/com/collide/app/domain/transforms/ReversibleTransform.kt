package com.collide.app.domain.transforms

import com.collide.app.domain.model.TransformSpec

enum class ApplicabilityStatus {
    APPLICABLE,
    NOT_APPLICABLE
}

data class TransformOutput(
    val bytes: ByteArray,
    val metadata: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TransformOutput
        if (!bytes.contentEquals(other.bytes)) return false
        if (!metadata.contentEquals(other.metadata)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + metadata.contentHashCode()
        return result
    }
}

interface ReversibleTransform {
    val spec: TransformSpec

    fun checkApplicability(input: ByteArray): ApplicabilityStatus

    /** Returns encoded bytes + metadata needed for decoding. */
    fun encode(input: ByteArray): TransformOutput

    /** Reconstructs the original bytes from encoded bytes + metadata. */
    fun decode(encoded: ByteArray, metadata: ByteArray): ByteArray

    fun humanSummary(): String = spec.displayName
}
