package com.aura.shell.command

import kotlin.math.abs

/**
 * Lightweight fuzzy matching for typos (local, no ML).
 */
object FuzzyMatcher {

    /**
     * Returns 0–1000+ style score for how well [token] matches [candidate] (word or label fragment).
     */
    fun fuzzyTokenScore(token: String, candidate: String): Int {
        if (token.isEmpty() || candidate.isEmpty()) return 0
        val t = token.lowercase()
        val c = candidate.lowercase()
        if (t == c) return 950
        if (c.startsWith(t)) return 800 - (c.length - t.length).coerceIn(0, 50)
        if (c.contains(t)) return 600

        val dist = levenshtein(t, c)
        val maxLen = maxOf(t.length, c.length)
        if (maxLen == 0) return 0
        // Penalize distance heavily for short strings
        val maxAllowed = when {
            t.length <= 3 -> 1
            t.length <= 5 -> 2
            else -> 3
        }
        if (dist > maxAllowed) return 0
        val similarity = 1000 - dist * 180 - abs(t.length - c.length) * 15
        return similarity.coerceAtLeast(0)
    }

    /**
     * Best fuzzy score of [token] against any word in [labelWords].
     */
    fun bestWordScore(token: String, labelWords: List<String>): Int {
        return labelWords.maxOfOrNull { w -> fuzzyTokenScore(token, w) } ?: 0
    }

    fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val m = a.length
        val n = b.length
        var prev = IntArray(n + 1) { it }
        for (i in 1..m) {
            val cur = IntArray(n + 1)
            cur[0] = i
            val ca = a[i - 1]
            for (j in 1..n) {
                val cost = if (ca == b[j - 1]) 0 else 1
                cur[j] = minOf(
                    cur[j - 1] + 1,
                    prev[j] + 1,
                    prev[j - 1] + cost,
                )
            }
            prev = cur
        }
        return prev[n]
    }
}
