package com.collide.app.domain.recipes

import com.collide.app.domain.model.BackendCompressor
import com.collide.app.domain.model.RecipeSpec
import com.collide.app.domain.model.RunMode
import com.collide.app.domain.transforms.TransformRegistry

/**
 * Deterministic candidate recipe generator (Phase 2).
 *
 * Rules:
 * - Enumeration order is stable and reproducible.
 * - No randomness, no ML.
 * - Supports chain lengths 1..4.
 * - Enabled-transform filtering: if [enabledTransformIds] is non-empty, only those transforms
 *   are used; otherwise all non-identity transforms are used.
 * - Consecutive-identical-transform deduplication: same transform may not appear at positions i and i+1.
 * - Total candidate count is bounded by maxCandidates.
 */
class CandidateGenerator(
    private val enabledTransformIds: Set<String> = emptySet()
) {

    fun generate(
        runMode: RunMode,
        maxCandidatesOverride: Int? = null,
        maxChainLengthOverride: Int? = null
    ): List<RecipeSpec> {
        val maxCandidates = maxCandidatesOverride ?: runMode.maxCandidates
        val maxChainLength = (maxChainLengthOverride ?: runMode.maxChainLength).coerceAtMost(4)

        val allTransforms = TransformRegistry.allExcludingIdentity
        val transforms = if (enabledTransformIds.isEmpty()) allTransforms
                         else allTransforms.filter { it.spec.id in enabledTransformIds }

        val backends = BackendCompressor.entries.toList()
        val candidates = mutableListOf<RecipeSpec>()
        var seqId = 0

        fun add(ids: List<String>, backend: BackendCompressor): Boolean {
            if (candidates.size >= maxCandidates) return false
            candidates.add(RecipeSpec("r${seqId++}", ids, backend))
            return true
        }

        fun consecutiveDup(ids: List<String>): Boolean {
            for (i in 0 until ids.size - 1) {
                if (ids[i] == ids[i + 1]) return true
            }
            return false
        }

        // Chain length 1
        for (t in transforms) {
            for (b in backends) {
                if (!add(listOf(t.spec.id), b)) return candidates
            }
        }

        // Chain length 2
        if (maxChainLength >= 2) {
            for (t1 in transforms) {
                for (t2 in transforms) {
                    if (t1.spec.id == t2.spec.id) continue
                    for (b in backends) {
                        if (!add(listOf(t1.spec.id, t2.spec.id), b)) return candidates
                    }
                }
            }
        }

        // Chain length 3
        if (maxChainLength >= 3) {
            for (t1 in transforms) {
                for (t2 in transforms) {
                    if (t1.spec.id == t2.spec.id) continue
                    for (t3 in transforms) {
                        if (t3.spec.id == t2.spec.id) continue
                        for (b in backends) {
                            if (!add(listOf(t1.spec.id, t2.spec.id, t3.spec.id), b)) return candidates
                        }
                    }
                }
            }
        }

        // Chain length 4 (enabled via Burst mode or explicit override)
        if (maxChainLength >= 4) {
            for (t1 in transforms) {
                for (t2 in transforms) {
                    if (t1.spec.id == t2.spec.id) continue
                    for (t3 in transforms) {
                        if (t3.spec.id == t2.spec.id) continue
                        for (t4 in transforms) {
                            if (t4.spec.id == t3.spec.id) continue
                            val chain = listOf(t1.spec.id, t2.spec.id, t3.spec.id, t4.spec.id)
                            if (consecutiveDup(chain)) continue
                            for (b in backends) {
                                if (!add(chain, b)) return candidates
                            }
                        }
                    }
                }
            }
        }

        return candidates
    }
}
