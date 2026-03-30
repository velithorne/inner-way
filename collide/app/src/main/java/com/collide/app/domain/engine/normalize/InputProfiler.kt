package com.collide.app.domain.engine.normalize

import com.collide.app.domain.model.InputProfile
import com.collide.app.domain.model.InputSample
import com.collide.app.domain.model.LanguageHint
import com.collide.app.domain.model.NormalizationStatus
import com.collide.app.domain.model.ParseStatus

/**
 * Lightweight heuristic profiler for input text.
 * Does NOT parse code to a full AST — it applies keyword, punctuation,
 * and structural pattern matching to estimate a language profile.
 * All results are labeled as "estimated" — no certainty is claimed.
 */
class InputProfiler {

    fun profile(sample: InputSample): InputProfile {
        val text = sample.rawText
        val lines = text.lines()
        val tokens = roughTokenize(text)

        val (hint, confidence, keywords) = detectLanguageHint(text, tokens)

        return InputProfile(
            estimatedLanguage = hint,
            confidence = confidence,
            tokenCountEstimate = tokens.size,
            lineCount = lines.size,
            averageLineLength = if (lines.isEmpty()) 0f
                else lines.sumOf { it.length }.toFloat() / lines.size,
            bracketDepthMax = computeMaxBracketDepth(text),
            keywordsDetected = keywords,
            parseStatus = ParseStatus.NOT_ATTEMPTED,
            normalizationStatus = NormalizationStatus.NOT_ATTEMPTED
        )
    }

    private fun roughTokenize(text: String): List<String> {
        return text
            .replace('\n', ' ')
            .replace('\t', ' ')
            .split(Regex("""[\s,;:()\{}\[\]<>]+"""))
            .filter { it.isNotBlank() }
    }

    private fun computeMaxBracketDepth(text: String): Int {
        var depth = 0
        var maxDepth = 0
        for (ch in text) {
            when (ch) {
                '{', '(', '[' -> { depth++; if (depth > maxDepth) maxDepth = depth }
                '}', ')', ']' -> if (depth > 0) depth--
            }
        }
        return maxDepth
    }

    private data class DetectionResult(
        val hint: LanguageHint,
        val confidence: Float,
        val keywords: List<String>
    )

    private fun detectLanguageHint(text: String, tokens: List<String>): DetectionResult {
        val scores = mutableMapOf<LanguageHint, Float>()
        val foundKeywords = mutableListOf<String>()

        val lower = text.lowercase()

        // JSON detection — simple: starts with { or [, has key: value pattern
        if ((lower.trimStart().startsWith("{") || lower.trimStart().startsWith("[")) &&
            lower.contains("\":")) {
            scores[LanguageHint.JSON_LIKE] = 0.85f
        }

        // Python-like: def, import, elif, indent-based, no semicolons
        val pythonKeywords = listOf("def ", "elif ", "import ", "from ", "class ", "self.", "None", "True", "False", "lambda")
        val pythonHits = pythonKeywords.count { lower.contains(it.lowercase()) }
        val hasSemicolons = text.contains(';')
        if (pythonHits >= 2) {
            scores[LanguageHint.PYTHON_LIKE] = 0.3f + (pythonHits * 0.1f).coerceAtMost(0.5f)
            if (!hasSemicolons) scores[LanguageHint.PYTHON_LIKE] =
                (scores[LanguageHint.PYTHON_LIKE]!! + 0.1f).coerceAtMost(0.9f)
            foundKeywords.addAll(pythonKeywords.filter { lower.contains(it.lowercase()) })
        }

        // JavaScript-like: function, var/let/const, =>, console, null, undefined
        val jsKeywords = listOf("function ", "const ", "let ", "var ", "=>", "console.", "null", "undefined", "return ", "typeof ")
        val jsHits = jsKeywords.count { text.contains(it) }
        if (jsHits >= 2) {
            scores[LanguageHint.JAVASCRIPT_LIKE] = 0.3f + (jsHits * 0.08f).coerceAtMost(0.55f)
            foundKeywords.addAll(jsKeywords.filter { text.contains(it) })
        }

        // Kotlin-like: fun, val, var, when, data class, object, companion
        val kotlinKeywords = listOf("fun ", "val ", "var ", "when ", "data class ", "object ", "companion ", "suspend ", "override ", "sealed ")
        val kotlinHits = kotlinKeywords.count { text.contains(it) }
        if (kotlinHits >= 2) {
            scores[LanguageHint.KOTLIN_LIKE] = 0.3f + (kotlinHits * 0.1f).coerceAtMost(0.55f)
            foundKeywords.addAll(kotlinKeywords.filter { text.contains(it) })
        }

        // C-like: #include, int main, ->, printf, struct, void, typedef
        val cKeywords = listOf("#include", "int main", "->", "printf(", "struct ", "void ", "typedef ", "return 0", "malloc(", "free(")
        val cHits = cKeywords.count { text.contains(it) }
        if (cHits >= 1) {
            scores[LanguageHint.C_LIKE] = 0.35f + (cHits * 0.1f).coerceAtMost(0.5f)
            foundKeywords.addAll(cKeywords.filter { text.contains(it) })
        }

        // Rule snippet: IF/THEN/WHEN/RULE/CONDITION style DSL
        val ruleKeywords = listOf("IF ", "THEN ", "WHEN ", "RULE ", "CONDITION:", "ACTION:", "ASSERT ", "MATCH ")
        val ruleHits = ruleKeywords.count { text.uppercase().contains(it) }
        if (ruleHits >= 2) {
            scores[LanguageHint.RULE_SNIPPET] = 0.4f + (ruleHits * 0.1f).coerceAtMost(0.4f)
            foundKeywords.addAll(ruleKeywords.filter { text.uppercase().contains(it) })
        }

        // Pseudo-code: step-like, numbered lines, ALGORITHM, BEGIN/END
        val pseudoKeywords = listOf("BEGIN", "END", "ALGORITHM", "STEP ", "INPUT:", "OUTPUT:", "PROCEDURE")
        val pseudoHits = pseudoKeywords.count { text.uppercase().contains(it) }
        if (pseudoHits >= 2) {
            scores[LanguageHint.PSEUDO_CODE] = 0.4f + (pseudoHits * 0.1f).coerceAtMost(0.4f)
        }

        // Plain text: low score for everything else
        if (scores.isEmpty()) {
            val wordCount = tokens.size
            if (wordCount < 5) return DetectionResult(LanguageHint.UNKNOWN, 0.3f, emptyList())
            return DetectionResult(LanguageHint.PLAIN_TEXT, 0.5f, emptyList())
        }

        val best = scores.maxByOrNull { it.value }!!
        return DetectionResult(best.key, best.value.coerceIn(0.1f, 0.95f), foundKeywords.distinct().take(8))
    }
}
