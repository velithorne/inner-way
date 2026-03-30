package com.collide.app.domain.transforms

import com.collide.app.domain.model.TransformSpec
import java.nio.ByteBuffer

/**
 * Zero-Run RLE: specialised encoder for zero bytes.
 *
 * Encoding scheme:
 *   - Non-zero byte b: emit b directly (b != 0x00)
 *   - Run of N zeros (N >= 2): emit 0x00, N (2-byte big-endian)
 *   - Single zero: emit 0x00, 0x01
 *
 * Metadata: 4-byte big-endian original length.
 *
 * Applicable only when the input contains at least one zero byte.
 */
class ZeroRunRLETransform : ReversibleTransform {
    override val spec = TransformSpec(
        id = "zero_run_rle",
        displayName = "Zero-Run RLE",
        estimatedCostMs = 1
    )

    override fun checkApplicability(input: ByteArray): ApplicabilityStatus {
        return if (input.any { it == 0x00.toByte() }) ApplicabilityStatus.APPLICABLE
        else ApplicabilityStatus.NOT_APPLICABLE
    }

    override fun encode(input: ByteArray): TransformOutput {
        val out = ArrayList<Byte>(input.size)
        var i = 0
        while (i < input.size) {
            if (input[i] == 0x00.toByte()) {
                var runLen = 1
                while (i + runLen < input.size && input[i + runLen] == 0x00.toByte() && runLen < 65535) {
                    runLen++
                }
                out.add(0x00)
                out.add((runLen shr 8).toByte())
                out.add((runLen and 0xFF).toByte())
                i += runLen
            } else {
                out.add(input[i])
                i++
            }
        }
        val metadata = ByteBuffer.allocate(4).putInt(input.size).array()
        return TransformOutput(out.toByteArray(), metadata)
    }

    override fun decode(encoded: ByteArray, metadata: ByteArray): ByteArray {
        val originalSize = ByteBuffer.wrap(metadata).int
        val out = ByteArray(originalSize)
        var outIdx = 0
        var i = 0
        while (i < encoded.size) {
            if (encoded[i] == 0x00.toByte()) {
                val runLen = ((encoded[i + 1].toInt() and 0xFF) shl 8) or (encoded[i + 2].toInt() and 0xFF)
                outIdx += runLen
                i += 3
            } else {
                out[outIdx++] = encoded[i]
                i++
            }
        }
        return out
    }

    override fun humanSummary() = "Zero-Run RLE (run-lengths for zero bytes)"
}
