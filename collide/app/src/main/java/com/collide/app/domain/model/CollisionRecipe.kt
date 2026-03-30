package com.collide.app.domain.model

/**
 * An ordered list of deterministic transformation operations applied to one or two inputs.
 * Every recipe must be serializable and replayable.
 */
data class CollisionRecipe(
    val id: String,
    val name: String,
    val description: String,
    val mode: CollisionMode,
    val operations: List<RecipeOperation>,
    val seed: Long = 0L
) {
    fun toSerializableKey(): String =
        "${id}|${mode.name}|${operations.joinToString(",") { it.operationId }}"
}

enum class CollisionMode {
    SINGLE_INPUT_MUTATION,
    DUAL_INPUT_RECOMBINATION
}

/**
 * A single named operation within a recipe.
 */
data class RecipeOperation(
    val operationId: String,
    val params: Map<String, String> = emptyMap()
) {
    companion object {
        // Single-input operations
        const val STRIP_WHITESPACE_COMMENTS = "strip_whitespace_comments"
        const val ABSTRACT_IDENTIFIERS = "abstract_identifiers"
        const val NORMALIZE_LITERALS = "normalize_literals"
        const val REORDER_SAFE_BLOCKS = "reorder_safe_blocks"
        const val MIRROR_CONDITIONS = "mirror_conditions"
        const val COMPRESS_REPEATED_TOKENS = "compress_repeated_tokens"
        const val SUBSTITUTE_OPERATORS = "substitute_operators"
        const val EXTRACT_COMMON_SCAFFOLD = "extract_common_scaffold"
        const val SIMPLIFY_DEAD_PATTERNS = "simplify_dead_patterns"

        // Dual-input operations
        const val SPLICE_BRANCH_B_INTO_A = "splice_branch_b_into_a"
        const val COMPARE_STRUCTURE = "compare_structure"
        const val EXTRACT_SHARED_SCAFFOLD = "extract_shared_scaffold"
        const val HYBRIDIZE_CONSTRUCTS = "hybridize_constructs"
        const val DETECT_CONTRADICTIONS = "detect_contradictions"
    }
}

/**
 * Defines allowed candidate budget per run mode.
 */
enum class RunMode(
    val label: String,
    val maxCandidates: Int,
    val searchDepth: Int,
    val pruningAggressive: Boolean,
    val runExpensiveDetectors: Boolean
) {
    SAFE("Safe", 20, 2, true, false),
    BALANCED("Balanced", 60, 4, false, true),
    BURST("Burst", 200, 6, false, true)
}
