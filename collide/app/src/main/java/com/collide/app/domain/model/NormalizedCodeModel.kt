package com.collide.app.domain.model

/**
 * Intermediate representation of a code input after normalization.
 * All fields are derived heuristically — this is not a standards-compliant AST.
 */
data class NormalizedCodeModel(
    val sourceId: String,
    val tokens: List<TokenUnit>,
    val blocks: List<BlockNode>,
    val operatorSignature: OperatorSignature,
    val representationVersion: Int = 1
) {
    val tokenCount: Int get() = tokens.size
    val blockCount: Int get() = blocks.size
    val depth: Int get() = if (blocks.isEmpty()) 0 else blocks.maxOf { it.depth }
}

/**
 * A single lexical token extracted from source text.
 */
data class TokenUnit(
    val index: Int,
    val text: String,
    val kind: TokenKind,
    val lineNumber: Int,
    val abstractedText: String = text  // identifier-abstracted form
)

enum class TokenKind {
    KEYWORD,
    IDENTIFIER,
    OPERATOR,
    LITERAL_NUMBER,
    LITERAL_STRING,
    DELIMITER,
    WHITESPACE,
    COMMENT,
    UNKNOWN
}

/**
 * A structural block node representing a logical unit (function, loop, condition, etc.).
 * Built heuristically from indentation and bracket structure.
 */
data class BlockNode(
    val id: String,
    val kind: BlockKind,
    val startLine: Int,
    val endLine: Int,
    val depth: Int,
    val tokenCount: Int,
    val childIds: List<String> = emptyList(),
    val label: String = ""
)

enum class BlockKind {
    FUNCTION_DEF,
    CONDITION_BRANCH,
    LOOP_BODY,
    ASSIGNMENT_BLOCK,
    EXPRESSION_STMT,
    RETURN_STMT,
    COMMENT_BLOCK,
    UNKNOWN_BLOCK
}

/**
 * High-level summary of operator/construct usage for quick comparison.
 */
data class OperatorSignature(
    val arithmeticOps: Int,
    val comparisonOps: Int,
    val logicalOps: Int,
    val assignmentOps: Int,
    val functionCallCount: Int,
    val conditionalCount: Int,
    val loopCount: Int,
    val returnCount: Int,
    val uniqueIdentifierCount: Int
) {
    fun toFingerprint(): String =
        "a$arithmeticOps-c$comparisonOps-l$logicalOps-a$assignmentOps-f$functionCallCount-" +
        "cond$conditionalCount-loop$loopCount-ret$returnCount-ids$uniqueIdentifierCount"
}
