package com.collide.app.domain.engine.normalize

import com.collide.app.domain.model.BlockKind
import com.collide.app.domain.model.BlockNode
import com.collide.app.domain.model.InputSample
import com.collide.app.domain.model.NormalizedCodeModel
import com.collide.app.domain.model.OperatorSignature
import com.collide.app.domain.model.TokenKind
import com.collide.app.domain.model.TokenUnit
import java.util.UUID

/**
 * Converts raw input text into a NormalizedCodeModel.
 *
 * This is a deterministic, heuristic normalizer — it does NOT produce a
 * standards-compliant AST. It builds a practical token stream and block tree
 * that is good enough for structural comparison and collision operations.
 *
 * Normalization is honest: partial results are marked partial, not silently dropped.
 */
class CodeNormalizer {

    fun normalize(sample: InputSample): NormalizedCodeModel {
        val tokens = tokenize(sample.rawText)
        val blocks = buildBlockTree(sample.rawText.lines(), tokens)
        val signature = computeSignature(tokens)

        return NormalizedCodeModel(
            sourceId = sample.id,
            tokens = tokens,
            blocks = blocks,
            operatorSignature = signature
        )
    }

    // -------------------------------------------------------------------------
    // Tokenization
    // -------------------------------------------------------------------------

    fun tokenize(text: String): List<TokenUnit> {
        val tokens = mutableListOf<TokenUnit>()
        var index = 0
        var lineNum = 1

        val chars = text.toCharArray()
        var i = 0
        while (i < chars.size) {
            val c = chars[i]

            when {
                // Newline
                c == '\n' -> { lineNum++; i++ }

                // Whitespace (not newline)
                c.isWhitespace() -> { i++ }

                // Single-line comment //
                c == '/' && i + 1 < chars.size && chars[i + 1] == '/' -> {
                    val start = i
                    while (i < chars.size && chars[i] != '\n') i++
                    val text2 = text.substring(start, i)
                    tokens.add(TokenUnit(index++, text2, TokenKind.COMMENT, lineNum))
                }

                // Single-line comment #
                c == '#' -> {
                    val start = i
                    while (i < chars.size && chars[i] != '\n') i++
                    val text2 = text.substring(start, i)
                    tokens.add(TokenUnit(index++, text2, TokenKind.COMMENT, lineNum))
                }

                // String literal " or '
                c == '"' || c == '\'' -> {
                    val quote = c
                    val start = i
                    i++
                    while (i < chars.size && chars[i] != quote) {
                        if (chars[i] == '\\') i++ // skip escape
                        i++
                    }
                    if (i < chars.size) i++
                    val lit = text.substring(start, i)
                    tokens.add(TokenUnit(index++, lit, TokenKind.LITERAL_STRING, lineNum,
                        abstractedText = "\"STR\""))
                }

                // Number literal
                c.isDigit() || (c == '-' && i + 1 < chars.size && chars[i + 1].isDigit()) -> {
                    val start = i
                    if (c == '-') i++
                    while (i < chars.size && (chars[i].isDigit() || chars[i] == '.')) i++
                    val num = text.substring(start, i)
                    tokens.add(TokenUnit(index++, num, TokenKind.LITERAL_NUMBER, lineNum,
                        abstractedText = "NUM"))
                }

                // Identifier or keyword
                c.isLetter() || c == '_' -> {
                    val start = i
                    while (i < chars.size && (chars[i].isLetterOrDigit() || chars[i] == '_')) i++
                    val word = text.substring(start, i)
                    val kind = if (isKeyword(word)) TokenKind.KEYWORD else TokenKind.IDENTIFIER
                    tokens.add(TokenUnit(index++, word, kind, lineNum,
                        abstractedText = if (kind == TokenKind.IDENTIFIER) abstractIdentifier(word) else word))
                }

                // Operators
                c == '=' || c == '+' || c == '-' || c == '*' || c == '/' ||
                c == '<' || c == '>' || c == '!' || c == '&' || c == '|' ||
                c == '%' || c == '^' || c == '~' -> {
                    val start = i
                    // Try two-char operators
                    if (i + 1 < chars.size) {
                        val two = text.substring(i, i + 2)
                        if (two in setOf("==", "!=", "<=", ">=", "&&", "||", "++", "--",
                                         "+=", "-=", "*=", "/=", "->", "=>")) {
                            tokens.add(TokenUnit(index++, two, TokenKind.OPERATOR, lineNum))
                            i += 2
                            continue
                        }
                    }
                    tokens.add(TokenUnit(index++, c.toString(), TokenKind.OPERATOR, lineNum))
                    i++
                }

                // Delimiters
                c == '(' || c == ')' || c == '{' || c == '}' ||
                c == '[' || c == ']' || c == ';' || c == ',' ||
                c == ':' || c == '.' -> {
                    tokens.add(TokenUnit(index++, c.toString(), TokenKind.DELIMITER, lineNum))
                    i++
                }

                else -> {
                    tokens.add(TokenUnit(index++, c.toString(), TokenKind.UNKNOWN, lineNum))
                    i++
                }
            }
        }
        return tokens
    }

    private val KEYWORDS = setOf(
        // General
        "if", "else", "for", "while", "do", "return", "break", "continue",
        "true", "false", "null", "None", "undefined",
        // Function/class
        "function", "def", "fun", "class", "struct", "interface", "enum",
        "val", "var", "let", "const", "int", "float", "double", "bool",
        "boolean", "string", "String", "void", "new", "this", "self",
        // Module
        "import", "from", "export", "include", "package",
        // OOP
        "extends", "implements", "override", "public", "private", "protected",
        "static", "final", "abstract", "sealed", "data", "object", "companion",
        // Logic
        "and", "or", "not", "in", "is", "as", "when", "match", "switch",
        "case", "default", "try", "catch", "throw", "finally",
        // Kotlin-specific
        "suspend", "coroutine", "lambda", "with", "apply", "let", "run", "also"
    )

    private fun isKeyword(word: String): Boolean = word in KEYWORDS

    private val identifierMap = mutableMapOf<String, String>()
    private var identifierCounter = 0

    private fun abstractIdentifier(name: String): String {
        if (name.length <= 1) return name
        return identifierMap.getOrPut(name) { "id_${identifierCounter++}" }
    }

    // -------------------------------------------------------------------------
    // Block tree construction — heuristic, indentation + bracket based
    // -------------------------------------------------------------------------

    fun buildBlockTree(lines: List<String>, tokens: List<TokenUnit>): List<BlockNode> {
        val blocks = mutableListOf<BlockNode>()
        var blockId = 0

        data class BlockAccum(
            val startLine: Int,
            val depth: Int,
            val kind: BlockKind,
            val label: String,
            val tokensBefore: Int
        )

        val stack = ArrayDeque<BlockAccum>()
        val tokensByLine = tokens.groupBy { it.lineNumber }

        for ((i, line) in lines.withIndex()) {
            val lineNum = i + 1
            val trimmed = line.trimStart()
            val indent = line.length - trimmed.length
            val depth = indent / 4  // assume 4-space indent

            val kind = detectBlockKind(trimmed)
            val label = extractBlockLabel(trimmed)

            val lineTokenCount = tokensByLine[lineNum]?.size ?: 0

            // Simple heuristic: open a new block on function/loop/condition definitions
            if (kind != BlockKind.EXPRESSION_STMT && kind != BlockKind.UNKNOWN_BLOCK) {
                stack.addLast(BlockAccum(lineNum, depth, kind, label, blocks.size))
            }

            // Close blocks where next non-empty line has lower indent
            val nextNonEmpty = lines.drop(i + 1).firstOrNull { it.isNotBlank() }
            val nextIndent = nextNonEmpty?.let { it.length - it.trimStart().length } ?: 0
            val nextDepth = nextIndent / 4

            while (stack.isNotEmpty() && stack.last().depth >= depth && nextDepth <= depth - 1) {
                val b = stack.removeLast()
                blocks.add(BlockNode(
                    id = "blk_${blockId++}",
                    kind = b.kind,
                    startLine = b.startLine,
                    endLine = lineNum,
                    depth = b.depth,
                    tokenCount = lineTokenCount,
                    label = b.label
                ))
                break
            }
        }

        // Flush remaining open blocks
        for (b in stack.reversed()) {
            blocks.add(BlockNode(
                id = "blk_${blockId++}",
                kind = b.kind,
                startLine = b.startLine,
                endLine = lines.size,
                depth = b.depth,
                tokenCount = 0,
                label = b.label
            ))
        }

        return blocks.sortedBy { it.startLine }
    }

    private fun detectBlockKind(trimmedLine: String): BlockKind {
        val l = trimmedLine.lowercase()
        return when {
            l.startsWith("def ") || l.startsWith("fun ") || l.startsWith("function ") ||
            Regex("""^(public|private|protected|static|suspend|override)\s.*\(""").containsMatchIn(l) ||
            l.matches(Regex("""^\w+\s*\(.*\)\s*\{?\s*$""")) && l.contains("(") -> BlockKind.FUNCTION_DEF

            l.startsWith("if ") || l.startsWith("if(") || l.startsWith("else") ||
            l.startsWith("when ") || l.startsWith("switch") || l.startsWith("case ") -> BlockKind.CONDITION_BRANCH

            l.startsWith("for ") || l.startsWith("for(") || l.startsWith("while ") ||
            l.startsWith("while(") || l.startsWith("do {") -> BlockKind.LOOP_BODY

            l.startsWith("return ") || l.startsWith("return;") -> BlockKind.RETURN_STMT

            l.startsWith("//") || l.startsWith("#") || l.startsWith("/*") -> BlockKind.COMMENT_BLOCK

            l.contains("=") && !l.contains("==") -> BlockKind.ASSIGNMENT_BLOCK

            else -> BlockKind.EXPRESSION_STMT
        }
    }

    private fun extractBlockLabel(trimmedLine: String): String {
        return trimmedLine.take(40).trim()
    }

    // -------------------------------------------------------------------------
    // Operator signature computation
    // -------------------------------------------------------------------------

    fun computeSignature(tokens: List<TokenUnit>): OperatorSignature {
        var arith = 0; var compare = 0; var logical = 0; var assign = 0
        var funcCalls = 0; var conditionals = 0; var loops = 0; var returns = 0

        val uniqueIds = tokens.filter { it.kind == TokenKind.IDENTIFIER }
            .map { it.text }.toSet().size

        for ((idx, tok) in tokens.withIndex()) {
            when {
                tok.kind == TokenKind.OPERATOR && tok.text in setOf("+", "-", "*", "/", "%", "^") -> arith++
                tok.kind == TokenKind.OPERATOR && tok.text in setOf("==", "!=", "<", ">", "<=", ">=") -> compare++
                tok.kind == TokenKind.OPERATOR && tok.text in setOf("&&", "||", "!") -> logical++
                tok.kind == TokenKind.OPERATOR && tok.text in setOf("=", "+=", "-=", "*=", "/=") -> assign++

                tok.kind == TokenKind.KEYWORD && tok.text in setOf("if", "else", "when", "switch", "case") -> conditionals++
                tok.kind == TokenKind.KEYWORD && tok.text in setOf("for", "while", "do") -> loops++
                tok.kind == TokenKind.KEYWORD && tok.text == "return" -> returns++

                // Function call heuristic: identifier followed by (
                tok.kind == TokenKind.IDENTIFIER &&
                idx + 1 < tokens.size && tokens[idx + 1].text == "(" -> funcCalls++
            }
        }

        return OperatorSignature(
            arithmeticOps = arith,
            comparisonOps = compare,
            logicalOps = logical,
            assignmentOps = assign,
            functionCallCount = funcCalls,
            conditionalCount = conditionals,
            loopCount = loops,
            returnCount = returns,
            uniqueIdentifierCount = uniqueIds
        )
    }
}
