package com.collide.app.domain.engine.collision

import com.collide.app.domain.model.BlockNode
import com.collide.app.domain.model.NormalizedCodeModel
import com.collide.app.domain.model.RecipeOperation
import com.collide.app.domain.model.TokenKind
import com.collide.app.domain.model.TokenUnit

/**
 * Applies individual transformation operations to normalized code models.
 *
 * All operations here are structural search operations — they are NOT guaranteed
 * semantic-preserving refactors. The app must not claim equivalence unless
 * a detector provides evidence.
 *
 * Every operation is deterministic and explainable.
 */
class TransformationEngine {

    /**
     * Apply a single operation to one or two normalized models.
     * Returns a text representation of the result.
     */
    fun applyOperation(
        op: RecipeOperation,
        modelA: NormalizedCodeModel,
        modelB: NormalizedCodeModel? = null
    ): TransformResult {
        return when (op.operationId) {
            RecipeOperation.STRIP_WHITESPACE_COMMENTS -> stripWhitespaceComments(modelA)
            RecipeOperation.ABSTRACT_IDENTIFIERS -> abstractIdentifiers(modelA)
            RecipeOperation.NORMALIZE_LITERALS -> normalizeLiterals(modelA)
            RecipeOperation.REORDER_SAFE_BLOCKS -> reorderSafeBlocks(modelA)
            RecipeOperation.MIRROR_CONDITIONS -> mirrorConditions(modelA)
            RecipeOperation.COMPRESS_REPEATED_TOKENS -> compressRepeatedTokens(modelA)
            RecipeOperation.SUBSTITUTE_OPERATORS -> substituteOperators(modelA, op.params)
            RecipeOperation.EXTRACT_COMMON_SCAFFOLD -> extractCommonScaffold(modelA)
            RecipeOperation.SIMPLIFY_DEAD_PATTERNS -> simplifyDeadPatterns(modelA)
            RecipeOperation.SPLICE_BRANCH_B_INTO_A -> spliceBranchBIntoA(modelA, modelB)
            RecipeOperation.COMPARE_STRUCTURE -> compareStructure(modelA, modelB)
            RecipeOperation.EXTRACT_SHARED_SCAFFOLD -> extractSharedScaffold(modelA, modelB)
            RecipeOperation.HYBRIDIZE_CONSTRUCTS -> hybridizeConstructs(modelA, modelB)
            RecipeOperation.DETECT_CONTRADICTIONS -> detectContradictions(modelA, modelB)
            else -> TransformResult.Failure("Unknown operation: ${op.operationId}")
        }
    }

    /**
     * Apply a full recipe as a pipeline, chaining text outputs.
     */
    fun applyRecipe(
        recipe: com.collide.app.domain.model.CollisionRecipe,
        modelA: NormalizedCodeModel,
        modelB: NormalizedCodeModel?
    ): TransformResult {
        var currentText = tokensToText(modelA.tokens)
        var notes = mutableListOf<String>()

        for (op in recipe.operations) {
            val result = applyOperation(op, modelA, modelB)
            when (result) {
                is TransformResult.Success -> {
                    currentText = result.text
                    notes.add("${op.operationId}: ok (${result.text.length} chars)")
                }
                is TransformResult.Partial -> {
                    currentText = result.text
                    notes.add("${op.operationId}: partial — ${result.reason}")
                }
                is TransformResult.Failure -> {
                    return TransformResult.Failure("${op.operationId} failed: ${result.reason}")
                }
            }
        }
        return TransformResult.Success(currentText, notes.joinToString("; "))
    }

    // -------------------------------------------------------------------------
    // Single-input operations
    // -------------------------------------------------------------------------

    private fun stripWhitespaceComments(model: NormalizedCodeModel): TransformResult {
        val filtered = model.tokens.filter {
            it.kind != TokenKind.COMMENT && it.kind != TokenKind.WHITESPACE
        }
        return TransformResult.Success(tokensToText(filtered), "stripped ${model.tokens.size - filtered.size} tokens")
    }

    private fun abstractIdentifiers(model: NormalizedCodeModel): TransformResult {
        val renamed = model.tokens.map { tok ->
            if (tok.kind == TokenKind.IDENTIFIER) tok.copy(text = tok.abstractedText)
            else tok
        }
        return TransformResult.Success(tokensToText(renamed), "abstracted identifiers")
    }

    private fun normalizeLiterals(model: NormalizedCodeModel): TransformResult {
        val normed = model.tokens.map { tok ->
            when (tok.kind) {
                TokenKind.LITERAL_NUMBER -> tok.copy(text = "0")
                TokenKind.LITERAL_STRING -> tok.copy(text = "\"\"")
                else -> tok
            }
        }
        return TransformResult.Success(tokensToText(normed), "normalized literals")
    }

    private fun reorderSafeBlocks(model: NormalizedCodeModel): TransformResult {
        // Heuristic: reorder independent top-level assignment blocks only
        val topLevelAssignments = model.blocks.filter {
            it.depth == 0 && it.kind == com.collide.app.domain.model.BlockKind.ASSIGNMENT_BLOCK
        }
        if (topLevelAssignments.size < 2) {
            return TransformResult.Partial(
                tokensToText(model.tokens),
                "fewer than 2 reorderable blocks found"
            )
        }
        // Simple: reverse assignment blocks in token stream
        val tokenText = tokensToText(model.tokens)
        val lines = tokenText.lines().toMutableList()
        val assignLineRanges = topLevelAssignments.map { it.startLine..it.endLine }
        // Swap first two assignment block line ranges
        val r1 = assignLineRanges[0]
        val r2 = assignLineRanges[1]
        if (r1.last < r2.first && r2.last <= lines.size) {
            val block1 = lines.subList(r1.first - 1, r1.last).toList()
            val block2 = lines.subList(r2.first - 1, r2.last).toList()
            for ((i, l) in block2.withIndex()) { lines[r1.first - 1 + i] = l }
            for ((i, l) in block1.withIndex()) { lines[r2.first - 1 + i] = l }
        }
        return TransformResult.Success(lines.joinToString("\n"), "reordered ${topLevelAssignments.size} blocks")
    }

    private fun mirrorConditions(model: NormalizedCodeModel): TransformResult {
        val operatorMirrorMap = mapOf(
            ">" to "<=", "<" to ">=",
            ">=" to "<", "<=" to ">",
            "==" to "!=", "!=" to "=="
        )
        val mirrored = model.tokens.map { tok ->
            if (tok.kind == TokenKind.OPERATOR && tok.text in operatorMirrorMap) {
                tok.copy(text = operatorMirrorMap[tok.text]!!)
            } else tok
        }
        val changedCount = model.tokens.zip(mirrored).count { (a, b) -> a.text != b.text }
        return if (changedCount > 0) {
            TransformResult.Success(tokensToText(mirrored), "mirrored $changedCount operators")
        } else {
            TransformResult.Partial(tokensToText(model.tokens), "no mirrorable operators found")
        }
    }

    private fun compressRepeatedTokens(model: NormalizedCodeModel): TransformResult {
        if (model.tokens.isEmpty()) return TransformResult.Partial("", "empty token stream")

        val compressed = mutableListOf<TokenUnit>()
        var i = 0
        var compressionCount = 0
        val tokens = model.tokens

        while (i < tokens.size) {
            // Look for runs of 3+ identical tokens
            val tok = tokens[i]
            var runLen = 1
            while (i + runLen < tokens.size && tokens[i + runLen].text == tok.text) runLen++
            if (runLen >= 3) {
                compressed.add(tok.copy(text = "${tok.text}×${runLen}"))
                compressionCount++
                i += runLen
            } else {
                compressed.add(tok)
                i++
            }
        }
        return if (compressionCount > 0) {
            TransformResult.Success(tokensToText(compressed), "compressed $compressionCount runs")
        } else {
            TransformResult.Partial(tokensToText(model.tokens), "no repetitions found")
        }
    }

    private fun substituteOperators(model: NormalizedCodeModel, params: Map<String, String>): TransformResult {
        val family = params["family"] ?: "arithmetic"
        val substitutionMap: Map<String, String> = when (family) {
            "arithmetic" -> mapOf("+" to "-", "-" to "+")
            "comparison" -> mapOf(">" to ">=", "<" to "<=")
            "logical" -> mapOf("&&" to "||", "||" to "&&")
            else -> emptyMap()
        }
        val substituted = model.tokens.map { tok ->
            if (tok.kind == TokenKind.OPERATOR && tok.text in substitutionMap) {
                tok.copy(text = substitutionMap[tok.text]!!)
            } else tok
        }
        val changedCount = model.tokens.zip(substituted).count { (a, b) -> a.text != b.text }
        return if (changedCount > 0) {
            TransformResult.Success(tokensToText(substituted), "substituted $changedCount operators in family '$family'")
        } else {
            TransformResult.Partial(tokensToText(model.tokens), "no operators to substitute in family '$family'")
        }
    }

    private fun extractCommonScaffold(model: NormalizedCodeModel): TransformResult {
        // Find token n-grams that repeat, represent as SCAFFOLD annotation
        val ngrams = mutableMapOf<String, Int>()
        val n = 3
        val tokens = model.tokens.filter { it.kind != TokenKind.COMMENT }

        for (i in 0..tokens.size - n) {
            val gram = tokens.subList(i, i + n).joinToString(" ") { it.abstractedText }
            ngrams[gram] = (ngrams[gram] ?: 0) + 1
        }
        val repeated = ngrams.filter { it.value >= 2 }
        return if (repeated.isNotEmpty()) {
            val topScaffolds = repeated.entries
                .sortedByDescending { it.value }
                .take(3)
                .joinToString("\n") { "SCAFFOLD(×${it.value}): ${it.key}" }
            TransformResult.Success(topScaffolds, "found ${repeated.size} repeated n-grams")
        } else {
            TransformResult.Partial(tokensToText(model.tokens), "no repeated scaffolds found")
        }
    }

    private fun simplifyDeadPatterns(model: NormalizedCodeModel): TransformResult {
        val tokens = model.tokens.toMutableList()
        var removed = 0

        // Pattern: x = x (self-assignment)
        val result = mutableListOf<TokenUnit>()
        var i = 0
        while (i < tokens.size - 4) {
            val t0 = tokens[i]; val t1 = tokens[i + 1]; val t2 = tokens[i + 2]
            if (t0.kind == TokenKind.IDENTIFIER && t1.text == "=" && t1.kind == TokenKind.OPERATOR
                && t2.kind == TokenKind.IDENTIFIER && t0.abstractedText == t2.abstractedText) {
                removed++
                i += 3
            } else {
                result.add(tokens[i]); i++
            }
        }
        while (i < tokens.size) { result.add(tokens[i]); i++ }

        return if (removed > 0) {
            TransformResult.Success(tokensToText(result), "removed $removed self-assignment patterns")
        } else {
            TransformResult.Partial(tokensToText(model.tokens), "no dead patterns found")
        }
    }

    // -------------------------------------------------------------------------
    // Dual-input operations
    // -------------------------------------------------------------------------

    private fun spliceBranchBIntoA(modelA: NormalizedCodeModel, modelB: NormalizedCodeModel?): TransformResult {
        if (modelB == null) return TransformResult.Failure("dual-input operation requires input B")

        val blocksB = modelB.blocks.filter {
            it.kind == com.collide.app.domain.model.BlockKind.CONDITION_BRANCH ||
            it.kind == com.collide.app.domain.model.BlockKind.FUNCTION_DEF
        }
        if (blocksB.isEmpty()) return TransformResult.Partial(
            tokensToText(modelA.tokens), "no splíceable branches in input B"
        )

        val targetBlock = blocksB.first()
        val bTokensForBlock = modelB.tokens.filter {
            it.lineNumber in targetBlock.startLine..targetBlock.endLine
        }

        val splicePoint = modelA.tokens.indexOfFirst {
            it.kind == com.collide.app.domain.model.TokenKind.KEYWORD &&
            it.text in listOf("if", "else", "for", "while")
        }

        val spliced = if (splicePoint >= 0) {
            val before = modelA.tokens.subList(0, splicePoint)
            val after = modelA.tokens.subList(splicePoint, modelA.tokens.size)
            before + bTokensForBlock + after
        } else {
            modelA.tokens + listOf(
                TokenUnit(modelA.tokens.size, "\n// --- SPLICE FROM B ---\n",
                    TokenKind.COMMENT, 0)
            ) + bTokensForBlock
        }

        return TransformResult.Success(tokensToText(spliced),
            "spliced ${bTokensForBlock.size} tokens from B block '${targetBlock.label.take(20)}'")
    }

    private fun compareStructure(modelA: NormalizedCodeModel, modelB: NormalizedCodeModel?): TransformResult {
        if (modelB == null) return TransformResult.Failure("dual-input operation requires input B")

        val sigA = modelA.operatorSignature
        val sigB = modelB.operatorSignature

        val comparison = buildString {
            appendLine("// === STRUCTURE COMPARISON ===")
            appendLine("// INPUT A: tokens=${modelA.tokenCount}, blocks=${modelA.blockCount}")
            appendLine("//   sig: ${sigA.toFingerprint()}")
            appendLine("// INPUT B: tokens=${modelB.tokenCount}, blocks=${modelB.blockCount}")
            appendLine("//   sig: ${sigB.toFingerprint()}")
            appendLine("//")
            appendLine("// DIVERGENCES:")
            if (sigA.conditionalCount != sigB.conditionalCount)
                appendLine("//   conditionals: A=${sigA.conditionalCount} vs B=${sigB.conditionalCount}")
            if (sigA.loopCount != sigB.loopCount)
                appendLine("//   loops: A=${sigA.loopCount} vs B=${sigB.loopCount}")
            if (sigA.functionCallCount != sigB.functionCallCount)
                appendLine("//   function_calls: A=${sigA.functionCallCount} vs B=${sigB.functionCallCount}")
            if (sigA.uniqueIdentifierCount != sigB.uniqueIdentifierCount)
                appendLine("//   unique_ids: A=${sigA.uniqueIdentifierCount} vs B=${sigB.uniqueIdentifierCount}")
            append("// SHARED_STRUCTURE_SCORE: ${computeStructuralSimilarity(modelA, modelB)}")
        }

        return TransformResult.Success(comparison, "structural comparison complete")
    }

    private fun extractSharedScaffold(modelA: NormalizedCodeModel, modelB: NormalizedCodeModel?): TransformResult {
        if (modelB == null) return TransformResult.Failure("dual-input operation requires input B")

        val abstractA = modelA.tokens.filter { it.kind != TokenKind.COMMENT }
            .map { it.abstractedText }.toSet()
        val abstractB = modelB.tokens.filter { it.kind != TokenKind.COMMENT }
            .map { it.abstractedText }.toSet()

        val shared = abstractA.intersect(abstractB).toList().sorted()
        val onlyA = (abstractA - abstractB).toList().sorted().take(10)
        val onlyB = (abstractB - abstractA).toList().sorted().take(10)

        val scaffold = buildString {
            appendLine("// === SHARED SCAFFOLD ===")
            appendLine("// Shared tokens (${shared.size}): ${shared.take(20).joinToString(", ")}")
            appendLine("// Only in A (${onlyA.size}): ${onlyA.joinToString(", ")}")
            appendLine("// Only in B (${onlyB.size}): ${onlyB.joinToString(", ")}")
            appendLine("SCAFFOLD {")
            for (tok in shared.take(15)) appendLine("  $tok")
            append("}")
        }

        return TransformResult.Success(scaffold, "shared ${shared.size} tokens, diverged A=${onlyA.size} B=${onlyB.size}")
    }

    private fun hybridizeConstructs(modelA: NormalizedCodeModel, modelB: NormalizedCodeModel?): TransformResult {
        if (modelB == null) return TransformResult.Failure("dual-input operation requires input B")

        // Take function defs from A, loop bodies from B, conditions alternating
        val funcDefsA = modelA.tokens.take(modelA.tokens.size / 2)
        val loopBodyB = modelB.tokens.drop(modelB.tokens.size / 2)

        val hybrid = funcDefsA + listOf(
            TokenUnit(funcDefsA.size, "\n// --- HYBRID BOUNDARY ---\n", TokenKind.COMMENT, 0)
        ) + loopBodyB

        return TransformResult.Success(tokensToText(hybrid),
            "hybridized: ${funcDefsA.size} tokens from A head + ${loopBodyB.size} from B tail")
    }

    private fun detectContradictions(modelA: NormalizedCodeModel, modelB: NormalizedCodeModel?): TransformResult {
        if (modelB == null) return TransformResult.Failure("dual-input operation requires input B")

        val similarity = computeStructuralSimilarity(modelA, modelB)
        val sigA = modelA.operatorSignature
        val sigB = modelB.operatorSignature

        val contradictions = buildString {
            appendLine("// === CONTRADICTION ANALYSIS ===")
            appendLine("// Structural similarity: ${"%.2f".format(similarity)}")
            appendLine()
            if (similarity > 0.6f) {
                appendLine("// HIGH STRUCTURAL SIMILARITY — checking for behavioral divergence:")
                val opDiff = kotlin.math.abs(sigA.arithmeticOps - sigB.arithmeticOps) +
                             kotlin.math.abs(sigA.comparisonOps - sigB.comparisonOps)
                if (opDiff > 2) appendLine("//   TENSION: similar structure but op pattern differs by $opDiff")
                if (sigA.returnCount != sigB.returnCount)
                    appendLine("//   TENSION: different return counts — A=${sigA.returnCount} B=${sigB.returnCount}")
                if (sigA.loopCount == 0 && sigB.loopCount > 0)
                    appendLine("//   TENSION: A has no loops, B has ${sigB.loopCount}")
            } else {
                appendLine("// LOW STRUCTURAL SIMILARITY (${similarity}) — inputs are structurally different")
                appendLine("// No meaningful contradiction can be detected from dissimilar inputs")
            }
            append("// CONTRADICTION_SCORE: ${"%.2f".format(1f - similarity)}")
        }

        return TransformResult.Success(contradictions, "contradiction analysis complete, similarity=${"%.2f".format(similarity)}")
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    fun tokensToText(tokens: List<TokenUnit>): String {
        val sb = StringBuilder()
        for ((i, tok) in tokens.withIndex()) {
            if (i > 0) {
                val prev = tokens[i - 1]
                val needsSpace = when {
                    tok.kind == TokenKind.DELIMITER && tok.text in listOf("(", ")", "{", "}", "[", "]", ";", ",") -> false
                    prev.kind == TokenKind.DELIMITER && prev.text in listOf("(", "[") -> false
                    tok.text == "." || prev.text == "." -> false
                    else -> true
                }
                if (needsSpace) sb.append(' ')
            }
            sb.append(tok.text)
        }
        return sb.toString()
    }

    fun computeStructuralSimilarity(modelA: NormalizedCodeModel, modelB: NormalizedCodeModel): Float {
        val sigA = modelA.operatorSignature
        val sigB = modelB.operatorSignature

        fun norm(a: Int, b: Int): Float = if (a + b == 0) 1f
            else 1f - (kotlin.math.abs(a - b).toFloat() / (a + b).toFloat())

        val scores = listOf(
            norm(sigA.conditionalCount, sigB.conditionalCount),
            norm(sigA.loopCount, sigB.loopCount),
            norm(sigA.functionCallCount, sigB.functionCallCount),
            norm(sigA.arithmeticOps, sigB.arithmeticOps),
            norm(sigA.returnCount, sigB.returnCount)
        )
        return scores.average().toFloat()
    }
}

sealed class TransformResult {
    data class Success(val text: String, val notes: String = "") : TransformResult()
    data class Partial(val text: String, val reason: String) : TransformResult()
    data class Failure(val reason: String) : TransformResult()

    fun textOrNull(): String? = when (this) {
        is Success -> text
        is Partial -> text
        is Failure -> null
    }
}
