package com.collide.app.domain.recipes

import com.collide.app.domain.model.BackendCompressor
import com.collide.app.domain.model.RecipeSpec
import com.collide.app.domain.model.RunMode
import com.collide.app.domain.transforms.TransformRegistry

/**
 * Deterministic candidate recipe generator.
 *
 * Generates recipe candidates by enumerating transform chains of length 1..maxChainLength
 * crossed with the available backend compressors. The ordering is stable and reproducible.
 *
 * No randomness, no ML, no hidden state.
 */
class CandidateGenerator {

    fun generate(
        runMode: RunMode,
        maxCandidatesOverride: Int? = null,
        maxChainLengthOverride: Int? = null
    ): List<RecipeSpec> {
        val maxCandidates = maxCandidatesOverride ?: runMode.maxCandidates
        val maxChainLength = maxChainLengthOverride ?: runMode.maxChainLength

        val transforms = TransformRegistry.allExcludingIdentity
        val backends = BackendCompressor.entries.toList()
        val candidates = mutableListOf<RecipeSpec>()
        var seqId = 0

        fun addIfRoom(spec: RecipeSpec) {
            if (candidates.size < maxCandidates) {
                candidates.add(spec)
            }
        }

        // Chain length 1
        for (t in transforms) {
            for (backend in backends) {
                addIfRoom(RecipeSpec("r${seqId++}", listOf(t.spec.id), backend))
            }
        }

        // Chain length 2
        if (maxChainLength >= 2) {
            for (t1 in transforms) {
                for (t2 in transforms) {
                    if (t1.spec.id == t2.spec.id) continue  // skip trivial same-same pairs
                    for (backend in backends) {
                        addIfRoom(RecipeSpec("r${seqId++}", listOf(t1.spec.id, t2.spec.id), backend))
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
                        for (backend in backends) {
                            addIfRoom(
                                RecipeSpec(
                                    "r${seqId++}",
                                    listOf(t1.spec.id, t2.spec.id, t3.spec.id),
                                    backend
                                )
                            )
                            if (candidates.size >= maxCandidates) return candidates
                        }
                    }
                }
            }
        }

        return candidates
    }
}
