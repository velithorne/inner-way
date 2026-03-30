package com.collide.app.domain.evaluator

import com.collide.app.domain.engine.BackendCompressorEngine
import com.collide.app.domain.model.CandidateClassification
import com.collide.app.domain.model.CandidateResult
import com.collide.app.domain.model.RecipeSpec
import com.collide.app.domain.transforms.ApplicabilityStatus
import com.collide.app.domain.transforms.TransformRegistry
import java.nio.ByteBuffer

/**
 * Evaluates a single candidate recipe against input data.
 *
 * Protocol:
 * 1. Check applicability of every transform in the chain.
 * 2. Encode through each transform sequentially.
 * 3. Run backend compressor.
 * 4. Compute total encoded size: compressed bytes + all metadata sections + header overhead.
 * 5. Decode backwards through each transform.
 * 6. Verify exact reconstruction.
 * 7. Compare total encoded size to baseline size.
 * 8. Classify result.
 *
 * Metadata overhead encoding:
 *   For each transform: [4-byte metadata_length][metadata_bytes]
 *   Header: [4-byte num_transforms][4-byte original_size]
 */
class CandidateEvaluator {

    data class EncodedPackage(
        val compressedData: ByteArray,
        val metadataBlocks: List<ByteArray>,
        val originalSize: Int
    ) {
        val totalSize: Long get() {
            var size = 4L + 4L // num_transforms header + original_size
            size += compressedData.size
            for (m in metadataBlocks) {
                size += 4 + m.size  // length prefix + data
            }
            return size
        }
    }

    fun evaluate(
        input: ByteArray,
        recipe: RecipeSpec,
        baselineSize: Long
    ): CandidateResult {
        val start = System.currentTimeMillis()

        // Collect transforms
        val transforms = recipe.transformIds.mapNotNull { id ->
            TransformRegistry.byId(id)
        }
        if (transforms.size != recipe.transformIds.size) {
            val elapsed = System.currentTimeMillis() - start
            return CandidateResult(
                recipeSpec = recipe,
                classification = CandidateClassification.ENCODE_ERROR,
                encodedSize = 0L,
                baselineSize = baselineSize,
                byteSavings = 0L,
                reconstructionPassed = false,
                elapsedMs = elapsed,
                errorMessage = "Unknown transform id in recipe"
            )
        }

        // Applicability check
        var buffer = input
        for (transform in transforms) {
            if (transform.checkApplicability(buffer) == ApplicabilityStatus.NOT_APPLICABLE) {
                val elapsed = System.currentTimeMillis() - start
                return CandidateResult(
                    recipeSpec = recipe,
                    classification = CandidateClassification.NOT_APPLICABLE,
                    encodedSize = 0L,
                    baselineSize = baselineSize,
                    byteSavings = 0L,
                    reconstructionPassed = false,
                    elapsedMs = elapsed,
                    errorMessage = "${transform.spec.displayName} not applicable"
                )
            }
        }

        // Encode phase
        val metadataBlocks = mutableListOf<ByteArray>()
        var current = input
        return try {
            for (transform in transforms) {
                val output = transform.encode(current)
                metadataBlocks.add(output.metadata)
                current = output.bytes
            }

            // Backend compress
            val compressed = BackendCompressorEngine.compress(current, recipe.backend)

            // Compute total encoded size
            val pkg = EncodedPackage(
                compressedData = compressed,
                metadataBlocks = metadataBlocks,
                originalSize = input.size
            )
            val totalEncodedSize = pkg.totalSize

            // Decode phase: decompress then reverse transforms
            val decompressed = BackendCompressorEngine.decompressUnknownSize(compressed)
            var decoded = decompressed
            for (i in transforms.indices.reversed()) {
                decoded = transforms[i].decode(decoded, metadataBlocks[i])
            }

            // Exactness check
            val passed = ExactnessVerifier.verify(input, decoded)
            val elapsed = System.currentTimeMillis() - start

            if (!passed) {
                return CandidateResult(
                    recipeSpec = recipe,
                    classification = CandidateClassification.EXACTNESS_FAILED,
                    encodedSize = totalEncodedSize,
                    baselineSize = baselineSize,
                    byteSavings = 0L,
                    reconstructionPassed = false,
                    elapsedMs = elapsed,
                    errorMessage = "Reconstruction mismatch"
                )
            }

            val savings = baselineSize - totalEncodedSize
            val classification = if (totalEncodedSize < baselineSize) {
                CandidateClassification.STRICT_WINNER
            } else {
                CandidateClassification.NO_STRICT_IMPROVEMENT
            }

            CandidateResult(
                recipeSpec = recipe,
                classification = classification,
                encodedSize = totalEncodedSize,
                baselineSize = baselineSize,
                byteSavings = savings,
                reconstructionPassed = true,
                elapsedMs = elapsed
            )
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - start
            CandidateResult(
                recipeSpec = recipe,
                classification = CandidateClassification.ENCODE_ERROR,
                encodedSize = 0L,
                baselineSize = baselineSize,
                byteSavings = 0L,
                reconstructionPassed = false,
                elapsedMs = elapsed,
                errorMessage = e.message ?: "Unknown error"
            )
        }
    }
}
