package com.aura.shell.command

import com.aura.shell.model.LauncherAppInfo

/**
 * Ranks apps for a target phrase with exact / alias / fuzzy tiers and confidence.
 */
class AppResolutionEngine(
    private val fuzzyMatcher: FuzzyMatcher = FuzzyMatcher,
) {

    data class Ranked(
        val app: LauncherAppInfo,
        val score: Int,
        val tier: MatchTier,
    )

    enum class MatchTier {
        EXACT_LABEL,
        ALIAS_SEMANTIC,
        FUZZY,
    }

    /**
     * Search / find apps — ranked list for "search apps for …".
     */
    fun searchApps(query: String, installedApps: List<LauncherAppInfo>): List<LauncherAppInfo> {
        val key = query.lowercase().trim()
        if (key.isEmpty()) return emptyList()
        return rankAllApps(installedApps, key).take(24).map { it.app }
    }

    fun resolve(
        target: String,
        installedApps: List<LauncherAppInfo>,
    ): ResolutionOutcome {
        val stripped = CommandNormalizer.stripTargetFiller(target)
        val key = stripped.lowercase().trim()
        if (key.isEmpty()) {
            return ResolutionOutcome.NoMatch("Nothing to open.")
        }

        // 1) Exact alias → semantic resolution
        CommandAliasRegistry.lookupAlias(key)?.let { semantics ->
            return resolveSemantics(semantics, installedApps, key)
        }

        // 2) Multi-word: try whole string as alias (e.g. "phone book")
        if (key.contains(' ')) {
            CommandAliasRegistry.lookupAlias(key)?.let { semantics ->
                return resolveSemantics(semantics, installedApps, key)
            }
        }

        // 3) Score all apps: exact + fuzzy on label tokens
        val ranked = rankAllApps(installedApps, key)
        return pickOutcome(ranked, key)
    }

    private fun resolveSemantics(
        semantics: List<CommandAliasRegistry.SemanticToken>,
        installedApps: List<LauncherAppInfo>,
        aliasKey: String,
    ): ResolutionOutcome {
        val mergedKeywords = semantics.flatMap { CommandAliasRegistry.keywordsFor(it).toList() }.toSet()
        val ranked = installedApps
            .map { app -> scoreSemantic(app, mergedKeywords) }
            .filter { it.score > 0 }
            .sortedByDescending { it.score }

        if (ranked.isEmpty()) {
            return ResolutionOutcome.NoMatch("No installed app matched \"$aliasKey\".")
        }

        val best = ranked[0]
        val second = ranked.getOrNull(1)
        val gap = if (second != null) best.score - second.score else Int.MAX_VALUE

        val canLaunch = best.score >= ResolutionThresholds.DIRECT_ALIAS_MIN &&
            gap >= ResolutionThresholds.GAP_FOR_DIRECT

        if (canLaunch) {
            return ResolutionOutcome.SingleLaunch(
                best.app,
                "Best match: ${best.app.label}",
            )
        }

        val kind = if (semantics.distinct().size > 1) {
            SuggestionKind.ALIAS_SPLIT
        } else {
            SuggestionKind.DID_YOU_MEAN
        }
        return ResolutionOutcome.Suggest(
            title = "Matches for \"$aliasKey\"",
            subtitle = "Tap to open",
            candidates = ranked.take(8).map { it.app },
            kind = kind,
        )
    }

    private fun scoreSemantic(
        app: LauncherAppInfo,
        keywords: Set<String>,
    ): Ranked {
        val label = normalizeLabel(app.label)
        val words = label.split(' ').filter { it.length >= 2 }
        var best = 0
        var tier = MatchTier.FUZZY
        for (kw in keywords) {
            val kwNorm = kw.lowercase()
            for (w in words) {
                if (w == kwNorm) {
                    best = maxOf(best, 920)
                    tier = MatchTier.ALIAS_SEMANTIC
                } else {
                    val fs = fuzzyMatcher.fuzzyTokenScore(kwNorm, w)
                    if (fs > best) {
                        best = fs
                        if (fs >= 750) tier = MatchTier.ALIAS_SEMANTIC
                    }
                }
            }
            if (label.contains(kwNorm)) {
                best = maxOf(best, 880)
                tier = MatchTier.ALIAS_SEMANTIC
            }
        }
        return Ranked(app, best, tier)
    }

    private fun rankAllApps(apps: List<LauncherAppInfo>, key: String): List<Ranked> {
        val tokens = key.split(' ').filter { it.isNotBlank() }
        return apps.mapNotNull { app ->
            val score = scoreAgainstLabel(app.label, tokens, key)
            if (score <= 0) null else Ranked(app, score, classifyTier(score))
        }.sortedByDescending { it.score }
    }

    private fun classifyTier(score: Int): MatchTier {
        return when {
            score >= 900 -> MatchTier.EXACT_LABEL
            score >= 700 -> MatchTier.ALIAS_SEMANTIC
            else -> MatchTier.FUZZY
        }
    }

    private fun scoreAgainstLabel(label: String, tokens: List<String>, fullKey: String): Int {
        val l = normalizeLabel(label)
        val words = l.split(' ').filter { it.isNotBlank() }
        val fk = fullKey.lowercase().replace(Regex("\\s+"), " ")

        if (l == fk) return 1000
        if (l.startsWith(fk)) return 920
        if (l.contains(fk)) return 880

        var sum = 0
        for (t in tokens) {
            if (t.length < 2) continue
            var bestTok = 0
            for (w in words) {
                if (w == t) bestTok = maxOf(bestTok, 900)
                else if (w.startsWith(t)) bestTok = maxOf(bestTok, 750)
                else if (w.contains(t)) bestTok = maxOf(bestTok, 650)
                else bestTok = maxOf(bestTok, fuzzyMatcher.fuzzyTokenScore(t, w))
            }
            sum += bestTok
        }
        if (tokens.isNotEmpty()) {
            sum /= tokens.size
        }
        return sum
    }

    private fun pickOutcome(ranked: List<Ranked>, queryKey: String): ResolutionOutcome {
        if (ranked.isEmpty()) {
            return ResolutionOutcome.NoMatch("No app matched \"$queryKey\". Try search apps for $queryKey")
        }
        val best = ranked[0]
        val second = ranked.getOrNull(1)

        val canDirect = when (best.tier) {
            MatchTier.EXACT_LABEL -> best.score >= ResolutionThresholds.DIRECT_EXACT_MIN
            MatchTier.ALIAS_SEMANTIC -> best.score >= ResolutionThresholds.DIRECT_ALIAS_MIN
            MatchTier.FUZZY -> best.score >= ResolutionThresholds.DIRECT_FUZZY_MIN &&
                (second == null || best.score - second.score >= ResolutionThresholds.GAP_FOR_DIRECT)
        }

        if (canDirect && (second == null || best.score - (second?.score ?: 0) >= 25)) {
            return ResolutionOutcome.SingleLaunch(
                best.app,
                when (best.tier) {
                    MatchTier.EXACT_LABEL -> "Best match: ${best.app.label}"
                    MatchTier.ALIAS_SEMANTIC -> "Best match: ${best.app.label}"
                    MatchTier.FUZZY -> "Close match: ${best.app.label}"
                },
            )
        }

        val topN = ranked.take(8).map { it.app }
        val kind = if (best.tier == MatchTier.FUZZY && best.score < ResolutionThresholds.DIRECT_FUZZY_MIN) {
            SuggestionKind.DID_YOU_MEAN
        } else {
            SuggestionKind.DISAMBIGUATION
        }
        return ResolutionOutcome.Suggest(
            title = if (kind == SuggestionKind.DID_YOU_MEAN) {
                "Did you mean one of these?"
            } else {
                "I found a few matches"
            },
            subtitle = "For \"$queryKey\"",
            candidates = topN,
            kind = kind,
        )
    }

    private fun normalizeLabel(label: String): String {
        return label.lowercase()
            .replace(Regex("[^a-z0-9 ]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }
}

sealed class ResolutionOutcome {
    data class SingleLaunch(
        val app: LauncherAppInfo,
        val hint: String,
    ) : ResolutionOutcome()

    data class Suggest(
        val title: String,
        val subtitle: String?,
        val candidates: List<LauncherAppInfo>,
        val kind: SuggestionKind,
    ) : ResolutionOutcome()

    data class NoMatch(val message: String) : ResolutionOutcome()
}

object ResolutionThresholds {
    const val DIRECT_EXACT_MIN = 850
    const val DIRECT_ALIAS_MIN = 780
    const val DIRECT_FUZZY_MIN = 720
    const val GAP_FOR_DIRECT = 80
}
