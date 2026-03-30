package com.collide.app.domain.transforms

import com.collide.app.domain.model.TransformSpec
import java.nio.ByteBuffer

/**
 * Run-Length Encoding on arbitrary byte runs.
 *
 * Encoding scheme:
 *   - If a run of N identical bytes (N >= 3): emit 0x00, N (2 bytes big-endian), byte_value
 *   - Otherwise: emit 0x01, raw_byte for each non-run byte
 *
 * Metadata: 4-byte big-endian original length so decode can allocate exactly.
 *
 * This transform is most useful on data with repeated byte sequences.
 * It is applicable only when the input has at least one run of length >= 3.
 */
class ByteRunRLETransform : ReversibleTransform {
    override val spec = TransformSpec(
        id = "byte_run_rle",
        displayName = "Byte-Run RLE",
        estimatedCostMs = 2
    )

    private val MIN_RUN = 3
    private val MAX_RUN = 65535

    override fun checkApplicability(input: ByteArray): ApplicabilityStatus {
        if (input.size < MIN_RUN) return ApplicabilityStatus.NOT_APPLICABLE
        var i = 0
        while (i < input.size) {
            var runLen = 1
            while (i + runLen < input.size && input[i + runLen] == input[i] && runLen < MAX_RUN) {
                runLen++
            }
            if (runLen >= MIN_RUN) return ApplicabilityStatus.APPLICABLE
            i += runLen
        }
        return ApplicabilityStatus.NOT_APPLICABLE
    }

    override fun encode(input: ByteArray): TransformOutput {
        val out = ArrayList<Byte>(input.size)
        var i = 0
        while (i < input.size) {
            var runLen = 1
            while (i + runLen < input.size && input[i + runLen] == input[i] && runLen < MAX_RUN) {
                runLen++
            }
            if (runLen >= MIN_RUN) {
                out.add(0x00)
                out.add((runLen shr 8).toByte())
                out.add((runLen and 0xFF).toByte())
                out.add(input[i])
                i += runLen
            } else {
                for (j in 0 until runLen) {
                    out.add(0x01)
                    out.add(input[i + j])
                }
                i += runLen
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
            when (encoded[i]) {
                0x00.toByte() -> {
                    val runLen = ((encoded[i + 1].toInt() and 0xFF) shl 8) or (encoded[i + 2].toInt() and 0xFF)
                    val value = encoded[i + 3]
                    for (j in 0 until runLen) out[outIdx++] = value
                    i += 4
                }
                0x01.toByte() -> {
                    out[outIdx++] = encoded[i + 1]
                    i += 2
                }
                else -> throw IllegalStateException("Unknown RLE opcode: ${encoded[i]} at position $i")
            }
        }
        return out
    }

    override fun humanSummary() = "Byte-Run RLE (min run $MIN_RUN)"
}
