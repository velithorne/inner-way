package com.collide.app.domain.transforms

import com.collide.app.domain.model.TransformSpec

/**
 * Delta-8 transform: replaces each byte with the difference from the previous byte.
 *
 * Useful for slowly-varying sequences (e.g. audio samples, gradients).
 * output[0] = input[0]
 * output[i] = (input[i] - input[i-1]) mod 256
 *
 * Decode is exact inverse:
 * decoded[0] = encoded[0]
 * decoded[i] = (encoded[i] + decoded[i-1]) mod 256
 *
 * No metadata needed; length is implicit from encoded array length.
 * Always applicable for input.size >= 2.
 */
class Delta8Transform : ReversibleTransform {
    override val spec = TransformSpec(
        id = "delta8",
        displayName = "Delta-8",
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
            out[i] = ((input[i].toInt() and 0xFF) - (input[i - 1].toInt() and 0xFF)).toByte()
        }
        return TransformOutput(out, ByteArray(0))
    }

    override fun decode(encoded: ByteArray, metadata: ByteArray): ByteArray {
        val out = ByteArray(encoded.size)
        out[0] = encoded[0]
        for (i in 1 until encoded.size) {
            out[i] = ((encoded[i].toInt() and 0xFF) + (out[i - 1].toInt() and 0xFF)).toByte()
        }
        return out
    }

    override fun humanSummary() = "Delta-8 (byte-level forward differences)"
}
