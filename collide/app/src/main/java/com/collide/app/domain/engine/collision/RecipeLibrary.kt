package com.collide.app.domain.engine.collision

import com.collide.app.domain.model.CollisionMode
import com.collide.app.domain.model.CollisionRecipe
import com.collide.app.domain.model.RecipeOperation

/**
 * Catalogue of available collision recipes for Phase 1.
 * All recipes are deterministic and bounded.
 */
object RecipeLibrary {

    val SINGLE_INPUT_RECIPES: List<CollisionRecipe> = listOf(
        CollisionRecipe(
            id = "s01",
            name = "Strip & Normalize",
            description = "Remove whitespace/comments, normalize literals, abstract identifiers",
            mode = CollisionMode.SINGLE_INPUT_MUTATION,
            operations = listOf(
                RecipeOperation(RecipeOperation.STRIP_WHITESPACE_COMMENTS),
                RecipeOperation(RecipeOperation.NORMALIZE_LITERALS),
                RecipeOperation(RecipeOperation.ABSTRACT_IDENTIFIERS)
            )
        ),
        CollisionRecipe(
            id = "s02",
            name = "Block Reorder",
            description = "Strip, then attempt safe block reordering by heuristic independence",
            mode = CollisionMode.SINGLE_INPUT_MUTATION,
            operations = listOf(
                RecipeOperation(RecipeOperation.STRIP_WHITESPACE_COMMENTS),
                RecipeOperation(RecipeOperation.REORDER_SAFE_BLOCKS)
            )
        ),
        CollisionRecipe(
            id = "s03",
            name = "Mirror Conditions",
            description = "Invert conditional branch structure while preserving logic shape",
            mode = CollisionMode.SINGLE_INPUT_MUTATION,
            operations = listOf(
                RecipeOperation(RecipeOperation.STRIP_WHITESPACE_COMMENTS),
                RecipeOperation(RecipeOperation.MIRROR_CONDITIONS)
            )
        ),
        CollisionRecipe(
            id = "s04",
            name = "Token Compression",
            description = "Compress repeated token sequences in representation",
            mode = CollisionMode.SINGLE_INPUT_MUTATION,
            operations = listOf(
                RecipeOperation(RecipeOperation.STRIP_WHITESPACE_COMMENTS),
                RecipeOperation(RecipeOperation.COMPRESS_REPEATED_TOKENS)
            )
        ),
        CollisionRecipe(
            id = "s05",
            name = "Operator Substitution",
            description = "Substitute operators within configured safe equivalence maps",
            mode = CollisionMode.SINGLE_INPUT_MUTATION,
            operations = listOf(
                RecipeOperation(RecipeOperation.STRIP_WHITESPACE_COMMENTS),
                RecipeOperation(RecipeOperation.SUBSTITUTE_OPERATORS,
                    mapOf("family" to "arithmetic"))
            )
        ),
        CollisionRecipe(
            id = "s06",
            name = "Scaffold Extraction",
            description = "Extract common reusable scaffold from repeated constructs",
            mode = CollisionMode.SINGLE_INPUT_MUTATION,
            operations = listOf(
                RecipeOperation(RecipeOperation.ABSTRACT_IDENTIFIERS),
                RecipeOperation(RecipeOperation.EXTRACT_COMMON_SCAFFOLD)
            )
        ),
        CollisionRecipe(
            id = "s07",
            name = "Dead Pattern Simplification",
            description = "Remove trivially dead or no-op patterns",
            mode = CollisionMode.SINGLE_INPUT_MUTATION,
            operations = listOf(
                RecipeOperation(RecipeOperation.STRIP_WHITESPACE_COMMENTS),
                RecipeOperation(RecipeOperation.SIMPLIFY_DEAD_PATTERNS)
            )
        ),
        CollisionRecipe(
            id = "s08",
            name = "Full Structural Normalize",
            description = "Full normalization pipeline: strip, abstract, normalize literals, compress",
            mode = CollisionMode.SINGLE_INPUT_MUTATION,
            operations = listOf(
                RecipeOperation(RecipeOperation.STRIP_WHITESPACE_COMMENTS),
                RecipeOperation(RecipeOperation.ABSTRACT_IDENTIFIERS),
                RecipeOperation(RecipeOperation.NORMALIZE_LITERALS),
                RecipeOperation(RecipeOperation.COMPRESS_REPEATED_TOKENS)
            )
        )
    )

    val DUAL_INPUT_RECIPES: List<CollisionRecipe> = listOf(
        CollisionRecipe(
            id = "d01",
            name = "Branch Splice",
            description = "Splice a branch from input B into a compatible region of input A",
            mode = CollisionMode.DUAL_INPUT_RECOMBINATION,
            operations = listOf(
                RecipeOperation(RecipeOperation.STRIP_WHITESPACE_COMMENTS),
                RecipeOperation(RecipeOperation.SPLICE_BRANCH_B_INTO_A)
            )
        ),
        CollisionRecipe(
            id = "d02",
            name = "Structure Compare",
            description = "Compare structural shape between inputs, extract divergences",
            mode = CollisionMode.DUAL_INPUT_RECOMBINATION,
            operations = listOf(
                RecipeOperation(RecipeOperation.ABSTRACT_IDENTIFIERS),
                RecipeOperation(RecipeOperation.COMPARE_STRUCTURE)
            )
        ),
        CollisionRecipe(
            id = "d03",
            name = "Shared Scaffold",
            description = "Extract structurally shared scaffold from both inputs",
            mode = CollisionMode.DUAL_INPUT_RECOMBINATION,
            operations = listOf(
                RecipeOperation(RecipeOperation.ABSTRACT_IDENTIFIERS),
                RecipeOperation(RecipeOperation.EXTRACT_SHARED_SCAFFOLD)
            )
        ),
        CollisionRecipe(
            id = "d04",
            name = "Hybridize Constructs",
            description = "Merge non-conflicting construct families from both inputs",
            mode = CollisionMode.DUAL_INPUT_RECOMBINATION,
            operations = listOf(
                RecipeOperation(RecipeOperation.STRIP_WHITESPACE_COMMENTS),
                RecipeOperation(RecipeOperation.HYBRIDIZE_CONSTRUCTS)
            )
        ),
        CollisionRecipe(
            id = "d05",
            name = "Contradiction Detector",
            description = "Identify structural contradictions between two similar-shaped inputs",
            mode = CollisionMode.DUAL_INPUT_RECOMBINATION,
            operations = listOf(
                RecipeOperation(RecipeOperation.ABSTRACT_IDENTIFIERS),
                RecipeOperation(RecipeOperation.DETECT_CONTRADICTIONS)
            )
        )
    )

    fun recipesFor(mode: CollisionMode): List<CollisionRecipe> = when (mode) {
        CollisionMode.SINGLE_INPUT_MUTATION -> SINGLE_INPUT_RECIPES
        CollisionMode.DUAL_INPUT_RECOMBINATION -> DUAL_INPUT_RECIPES
    }
}
