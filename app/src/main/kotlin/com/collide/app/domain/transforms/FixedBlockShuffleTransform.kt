package com.collide.app.domain.transforms

import com.collide.app.domain.model.TransformSpec
import java.nio.ByteBuffer

/**
 * Fixed-block shuffle (byte-plane interleave).
 *
 * For a chosen block size B, split the input into blocks of B bytes and
 * interleave by byte position within block (column-major → row-major reorder).
 *
 * Example with B=4 on 8 bytes [a0 a1 a2 a3 b0 b1 b2 b3]:
 *   Output: [a0 b0  a1 b1  a2 b2  a3 b3]
 *
 * This groups the Nth byte of every block together, which can improve
 * compressibility when each byte-plane has a narrower value range.
 *
 * Metadata: 4-byte big-endian block size | 4-byte big-endian original length
 *
 * Applicable only when input.size >= 2 * BLOCK_SIZE (at least two full blocks).
 * If trailing bytes don't fill a complete block they are appended unchanged after
 * the shuffled region. The metadata original-length allows exact reconstruction.
 */
class FixedBlockShuffleTransform(private val blockSize: Int = 4) : ReversibleTransform {
    init {
        require(blockSize in 2..256) { "blockSize must be in 2..256" }
    }

    override val spec = TransformSpec(
        id = "block_shuffle_$blockSize",
        displayName = "Block-Shuffle-$blockSize",
        estimatedCostMs = 2
    )

    override fun checkApplicability(input: ByteArray): ApplicabilityStatus {
        return if (input.size >= blockSize * 2) ApplicabilityStatus.APPLICABLE
        else ApplicabilityStatus.NOT_APPLICABLE
    }

    override fun encode(input: ByteArray): TransformOutput {
        val fullBlocks = input.size / blockSize
        val tail = input.size % blockSize
        val shuffledSize = fullBlocks * blockSize

        val out = ByteArray(input.size)
        // Interleave: for each byte position p within block, gather all blocks' p-th bytes
        for (p in 0 until blockSize) {
            for (b in 0 until fullBlocks) {
                out[p * fullBlocks + b] = input[b * blockSize + p]
            }
        }
        // Append trailing bytes unchanged
        if (tail > 0) {
            System.arraycopy(input, shuffledSize, out, shuffledSize, tail)
        }

        val metadata = ByteBuffer.allocate(8)
            .putInt(blockSize)
            .putInt(input.size)
            .array()
        return TransformOutput(out, metadata)
    }

    override fun decode(encoded: ByteArray, metadata: ByteArray): ByteArray {
        val buf = ByteBuffer.wrap(metadata)
        val storedBlockSize = buf.int
        val originalSize = buf.int

        val fullBlocks = originalSize / storedBlockSize
        val tail = originalSize % storedBlockSize

        val out = ByteArray(originalSize)
        // Reverse interleave
        for (p in 0 until storedBlockSize) {
            for (b in 0 until fullBlocks) {
                out[b * storedBlockSize + p] = encoded[p * fullBlocks + b]
            }
        }
        val shuffledSize = fullBlocks * storedBlockSize
        if (tail > 0) {
            System.arraycopy(encoded, shuffledSize, out, shuffledSize, tail)
        }
        return out
    }

    override fun humanSummary() = "Fixed-Block-Shuffle (block=$blockSize, byte-plane interleave)"
}
