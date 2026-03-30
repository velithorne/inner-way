package com.collide.app.domain.transforms

import com.collide.app.domain.model.TransformSpec

/**
 * Move-To-Front (MTF) transform.
 *
 * Maintains an alphabet list [0..255]. For each input byte, emit its current
 * rank (index) in the alphabet, then move that symbol to the front.
 *
 * This reduces recently-seen repeated bytes to low-rank values (small integers),
 * which can compress well with RLE or entropy coding afterward.
 *
 * Decode is the exact inverse: rebuild alphabet, look up rank → symbol, move to front.
 *
 * Always applicable for non-empty input.
 */
class MoveToFrontTransform : ReversibleTransform {
    override val spec = TransformSpec(
        id = "move_to_front",
        displayName = "Move-To-Front",
        estimatedCostMs = 2
    )

    override fun checkApplicability(input: ByteArray): ApplicabilityStatus {
        return if (input.isNotEmpty()) ApplicabilityStatus.APPLICABLE
        else ApplicabilityStatus.NOT_APPLICABLE
    }

    override fun encode(input: ByteArray): TransformOutput {
        val alphabet = IntArray(256) { it }
        val out = ByteArray(input.size)
        for (i in input.indices) {
            val sym = input[i].toInt() and 0xFF
            val rank = alphabet.indexOf(sym)
            out[i] = rank.toByte()
            // Move sym to front
            System.arraycopy(alphabet, 0, alphabet, 1, rank)
            alphabet[0] = sym
        }
        return TransformOutput(out, ByteArray(0))
    }

    override fun decode(encoded: ByteArray, metadata: ByteArray): ByteArray {
        val alphabet = IntArray(256) { it }
        val out = ByteArray(encoded.size)
        for (i in encoded.indices) {
            val rank = encoded[i].toInt() and 0xFF
            val sym = alphabet[rank]
            out[i] = sym.toByte()
            System.arraycopy(alphabet, 0, alphabet, 1, rank)
            alphabet[0] = sym
        }
        return out
    }

    override fun humanSummary() = "Move-To-Front (rank transform)"
}
