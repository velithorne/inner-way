package com.collide.app.domain.transforms

import com.collide.app.domain.model.TransformSpec

/**
 * XOR-previous-byte transform.
 *
 * output[0] = input[0]
 * output[i] = input[i] XOR input[i-1]
 *
 * Self-inverse for decode:
 * decoded[0] = encoded[0]
 * decoded[i] = encoded[i] XOR decoded[i-1]
 *
 * Applicable for input.size >= 2.
 */
class XorPrevByteTransform : ReversibleTransform {
    override val spec = TransformSpec(
        id = "xor_prev",
        displayName = "XOR-Prev-Byte",
        estimatedCostMs = 1
    )

    override fun checkApplicability(input: ByteArray): ApplicabilityStatus {
        return if (input.size >= 2) ApplicabilityStatus.APPLICABLE
        else ApplicabilityStatus.NOT_APPLICABLE
    }

    override fun encode(input: ByteArray): TransformOutput {
        val out = ByteArray(input.size)
        out[0] = input[0]
        for (i in 1 until input.size) {
            out[i] = (input[i].toInt() xor input[i - 1].toInt()).toByte()
        }
        return TransformOutput(out, ByteArray(0))
    }

    override fun decode(encoded: ByteArray, metadata: ByteArray): ByteArray {
        val out = ByteArray(encoded.size)
        out[0] = encoded[0]
        for (i in 1 until encoded.size) {
            out[i] = (encoded[i].toInt() xor out[i - 1].toInt()).toByte()
        }
        return out
    }

    override fun humanSummary() = "XOR-Prev-Byte (consecutive XOR differences)"
}
