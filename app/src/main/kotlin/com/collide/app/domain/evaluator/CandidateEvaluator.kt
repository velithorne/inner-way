package com.collide.app.domain.evaluator

import com.collide.app.domain.engine.BackendCompressorEngine
import com.collide.app.domain.model.*
import com.collide.app.domain.transforms.ApplicabilityStatus
import com.collide.app.domain.transforms.ReversibleTransform
import com.collide.app.domain.transforms.TransformRegistry

/**
 * Phase 2 CandidateEvaluator.
 *
 * Protocol:
 * 1. Resolve transforms; fail fast on unknown IDs.
 * 2. Applicability check on each transform in sequence (against the running buffer, not just input).
 * 3. Encode chain: collect per-transform metadata blobs.
 * 4. Backend compress.
 * 5. Build SizeBreakdown with every accounting field.
 * 6. Validate breakdown arithmetic (metadata accounting check).
 * 7. Decode chain in reverse.
 * 8. Full exactness + SHA-256 verification.
 * 9. Classify and return CandidateResult with all Phase 2 fields.
 *
 * Metadata wire format (same as Phase 1, explicit here for accounting):
 *   Container header : 4-byte num_transforms + 4-byte original_size = 8 bytes total
 *   Per transform    : 4-byte metadata_length + metadata_bytes
 */
class CandidateEvaluator(
    private val enabledTransformIds: Set<String> = emptySet() // empty = all enabled
) {

    fun evaluate(
        input: ByteArray,
        recipe: RecipeSpec,
        baselineSize: Long,
        candidateIndex: Int = -1
    ): CandidateResult {
        val start = System.currentTimeMillis()

        // 1. Resolve transforms
        val transforms = recipe.transformIds.mapNotNull { id -> TransformRegistry.byId(id) }
        if (transforms.size != recipe.transformIds.size) {
            return quickError(recipe, baselineSize, candidateIndex, start,
                CandidateClassification.ENCODE_ERROR, "Unknown transform id in recipe")
        }

        // 2. Applicability — checked against the running buffer as it evolves
        var buffer = input
        for (transform in transforms) {
            if (transform.checkApplicability(buffer) == ApplicabilityStatus.NOT_APPLICABLE) {
                val elapsed = elapsed(start)
                return CandidateResult(
                    recipeSpec = recipe,
                    classification = CandidateClassification.NOT_APPLICABLE,
                    encodedSize = 0L,
                    baselineSize = baselineSize,
                    byteSavings = 0L,
                    reconstructionPassed = false,
                    elapsedMs = elapsed,
                    reason = "${transform.spec.displayName} not applicable to current data",
                    candidateIndex = candidateIndex
                )
            }
            // Advance buffer to what the transform would produce (for next transform's applicability)
            // This is a dry check — we re-encode fully below, but we want accurate applicability.
            try {
                buffer = transform.encode(buffer).bytes
            } catch (_: Exception) {
                // If pre-check encode fails, fall through; the real encode below will catch it.
                break
            }
        }

        // 3–5. Encode chain
        return try {
            val metadataBlocks = mutableListOf<ByteArray>()
            var current = input
            for (transform in transforms) {
                val output = transform.encode(current)
                metadataBlocks.add(output.metadata)
                current = output.bytes
            }
            val transformedPayloadSize = current.size.toLong()

            // 4. Backend compress
            val compressed = BackendCompressorEngine.compress(current, recipe.backend)
            val backendCompressedSize = compressed.size.toLong()

            // 5. Build size breakdown
            val transformMetadataSize = metadataBlocks.fold(0L) { acc, m -> acc + 4L + m.size }
            val containerHeaderSize = 8L
            val breakdown = SizeBreakdown(
                transformedPayloadSize = transformedPayloadSize,
                backendCompressedPayloadSize = backendCompressedSize,
                transformMetadataSize = transformMetadataSize,
                containerHeaderSize = containerHeaderSize,
                originalInputSize = input.size.toLong()
            )
            val totalEncodedSize = breakdown.totalEncodedSize

            // 6. Metadata accounting sanity check
            if (totalEncodedSize <= 0) {
                return quickError(recipe, baselineSize, candidateIndex, start,
                    CandidateClassification.METADATA_ACCOUNTING_FAILURE,
                    "Total encoded size is non-positive: $totalEncodedSize")
            }

            // 7. Decode chain in reverse
            val decompressed = BackendCompressorEngine.decompressUnknownSize(compressed)
            var decoded = decompressed
            for (i in transforms.indices.reversed()) {
                decoded = transforms[i].decode(decoded, metadataBlocks[i])
            }

            // 8. Full Phase 2 verification
            val verification = ExactnessVerifier.verifyFull(input, decoded)
            val elapsed = elapsed(start)

            if (!verification.passed) {
                val cls = if (!verification.hashesMatch) CandidateClassification.HASH_MISMATCH
                          else CandidateClassification.EXACTNESS_FAILED
                return CandidateResult(
                    recipeSpec = recipe,
                    classification = cls,
                    encodedSize = totalEncodedSize,
                    baselineSize = baselineSize,
                    byteSavings = 0L,
                    reconstructionPassed = false,
                    elapsedMs = elapsed,
                    reason = verification.failureReason,
                    sizeBreakdown = breakdown,
                    verificationResult = verification,
                    candidateIndex = candidateIndex
                )
            }

            // 9. Classify
            val savings = baselineSize - totalEncodedSize
            val classification = if (totalEncodedSize < baselineSize)
                CandidateClassification.STRICT_WINNER
            else
                CandidateClassification.NO_STRICT_IMPROVEMENT

            CandidateResult(
                recipeSpec = recipe,
                classification = classification,
                encodedSize = totalEncodedSize,
                baselineSize = baselineSize,
                byteSavings = savings,
                reconstructionPassed = true,
                elapsedMs = elapsed,
                reason = if (classification == CandidateClassification.STRICT_WINNER)
                    "Saves ${savings}B vs baseline; verified exact reconstruction + SHA-256"
                else
                    "No size gain (${totalEncodedSize}B vs baseline ${baselineSize}B)",
                sizeBreakdown = breakdown,
                verificationResult = verification,
                candidateIndex = candidateIndex
            )
        } catch (e: Exception) {
            val cls = if (e.message?.contains("decompress", ignoreCase = true) == true ||
                         e.message?.contains("inflate", ignoreCase = true) == true)
                CandidateClassification.DECODE_ERROR
            else
                CandidateClassification.ENCODE_ERROR
            CandidateResult(
                recipeSpec = recipe,
                classification = cls,
                encodedSize = 0L,
                baselineSize = baselineSize,
                byteSavings = 0L,
                reconstructionPassed = false,
                elapsedMs = elapsed(start),
                reason = e.message ?: "Unknown error",
                candidateIndex = candidateIndex
            )
        }
    }

    private fun quickError(
        recipe: RecipeSpec,
        baselineSize: Long,
        idx: Int,
        start: Long,
        cls: CandidateClassification,
        reason: String
    ) = CandidateResult(
        recipeSpec = recipe,
        classification = cls,
        encodedSize = 0L,
        baselineSize = baselineSize,
        byteSavings = 0L,
        reconstructionPassed = false,
        elapsedMs = elapsed(start),
        reason = reason,
        candidateIndex = idx
    )

    private fun elapsed(start: Long) = System.currentTimeMillis() - start
}
