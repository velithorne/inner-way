package com.aura.shell.command

import com.aura.shell.model.LauncherAppInfo
import com.aura.shell.personalization.PersonalLearningBonus
import com.aura.shell.personalization.PersonalResolutionContext

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
    fun searchApps(
        query: String,
        installedApps: List<LauncherAppInfo>,
        personal: PersonalResolutionContext? = null,
    ): List<LauncherAppInfo> {
        val key = query.lowercase().trim()
        if (key.isEmpty()) return emptyList()
        val learnKey = personal?.learningStore?.normalizeQueryKey(query) ?: key
        return rankAllApps(installedApps, key, personal, learnKey).take(24).map { it.app }
    }

    fun resolve(
        target: String,
        installedApps: List<LauncherAppInfo>,
        personal: PersonalResolutionContext? = null,
    ): ResolutionOutcome {
        val stripped = CommandNormalizer.stripTargetFiller(target)
        val key = stripped.lowercase().trim()
        if (key.isEmpty()) {
            return ResolutionOutcome.NoMatch("Nothing to open.")
        }
        val learnKey = personal?.learningStore?.normalizeQueryKey(stripped) ?: key

        personal?.let { ctx ->
            resolvePersonalAlias(key, installedApps, ctx)?.let { return it }
        }

        // 1) Exact alias → semantic resolution
        CommandAliasRegistry.lookupAlias(key)?.let { semantics ->
            return resolveSemantics(semantics, installedApps, key, personal, learnKey)
        }

        // 2) Multi-word: try whole string as alias (e.g. "phone book")
        if (key.contains(' ')) {
            CommandAliasRegistry.lookupAlias(key)?.let { semantics ->
                return resolveSemantics(semantics, installedApps, key, personal, learnKey)
            }
        }

        // 3) Score all apps: exact + fuzzy on label tokens
        val ranked = rankAllApps(installedApps, key, personal, learnKey)
        return pickOutcome(ranked, key, personal, learnKey)
    }

    private fun resolvePersonalAlias(
        key: String,
        installedApps: List<LauncherAppInfo>,
        ctx: PersonalResolutionContext,
    ): ResolutionOutcome? {
        val pkg = ctx.aliasStore.getPackageForAlias(key) ?: return null
        val app = installedApps.find { it.packageName == pkg } ?: return ResolutionOutcome.NoMatch(
            "Your alias \"$key\" pointed to an app that isn’t installed. Edit it in Personalization.",
        )
        return ResolutionOutcome.SingleLaunch(
            app,
            "Using your alias for ${app.label}",
            usedPersonalAlias = true,
        )
    }

    private fun resolveSemantics(
        semantics: List<CommandAliasRegistry.SemanticToken>,
        installedApps: List<LauncherAppInfo>,
        aliasKey: String,
        personal: PersonalResolutionContext?,
        learnKey: String,
    ): ResolutionOutcome {
        // Prefer a single installed app whose visible label matches what the user said
        // (e.g. "messages" → app labeled "Messages"), so we launch instead of disambiguating
        // against Messenger / Facebook / carrier SMS duplicates.
        preferExactLabelForAlias(installedApps, aliasKey)?.let { return it }

        val mergedKeywords = semantics.flatMap { CommandAliasRegistry.keywordsFor(it).toList() }.toSet()
        val ranked = installedApps
            .map { app -> addPersonalBonus(scoreSemantic(app, mergedKeywords), app, personal, learnKey) }
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
            val learned = personal?.learningStore?.getWeight(learnKey, best.app.packageName) ?: 0
            val prefHint = if (PersonalLearningBonus.bonusPoints(learned) > 0) {
                "Preferred: ${best.app.label}"
            } else {
                "Best match: ${best.app.label}"
            }
            return ResolutionOutcome.SingleLaunch(
                best.app,
                prefHint,
                usedLearnedPreference = PersonalLearningBonus.bonusPoints(learned) > 0,
            )
        }

        val kind = if (semantics.distinct().size > 1) {
            SuggestionKind.ALIAS_SPLIT
        } else {
            SuggestionKind.DID_YOU_MEAN
        }
        return ResolutionOutcome.Suggest(
            title = "Matches for \"$aliasKey\"",
            subtitle = "Tap to choose",
            candidates = ranked.take(8).map { it.app },
            kind = kind,
        )
    }

    /**
     * Returns a single launch when exactly one app's normalized label matches [aliasKey]
     * or common singular/plural variants (messages ↔ message).
     */
    private fun preferExactLabelForAlias(
        installedApps: List<LauncherAppInfo>,
        aliasKey: String,
    ): ResolutionOutcome? {
        val key = aliasKey.trim().lowercase()
        if (key.isEmpty()) return null
        val variants = buildSet {
            add(key)
            if (key == "message") {
                add("messages")
                add("messaging")
            }
            if (key == "messages") {
                add("message")
                add("messaging")
            }
            if (key == "contact") add("contacts")
            if (key == "contacts") add("contact")
            if (key == "setting") add("settings")
            if (key == "settings") add("setting")
        }
        val matches = installedApps
            .filter { app -> normalizeLabel(app.label) in variants }
            .distinctBy { it.packageName }
        return when (matches.size) {
            1 -> ResolutionOutcome.SingleLaunch(
                matches[0],
                "Opened ${matches[0].label}",
            )
            else -> null
        }
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

    private fun rankAllApps(
        apps: List<LauncherAppInfo>,
        key: String,
        personal: PersonalResolutionContext?,
        learnKey: String,
    ): List<Ranked> {
        val tokens = key.split(' ').filter { it.isNotBlank() }
        return apps.mapNotNull { app ->
            val score = scoreAgainstLabel(app.label, tokens, key)
            if (score <= 0) null else addPersonalBonus(
                Ranked(app, score, classifyTier(score)),
                app,
                personal,
                learnKey,
            )
        }.sortedByDescending { it.score }
    }

    private fun addPersonalBonus(
        base: Ranked,
        app: LauncherAppInfo,
        personal: PersonalResolutionContext?,
        learnKey: String,
    ): Ranked {
        if (personal == null) return base
        val w = personal.learningStore.getWeight(learnKey, app.packageName)
        val bonus = PersonalLearningBonus.bonusPoints(w)
        if (bonus <= 0) return base
        val newScore = (base.score + bonus).coerceAtMost(1100)
        return base.copy(score = newScore, tier = maxOfTier(base.tier, classifyTier(newScore)))
    }

    private fun maxOfTier(a: MatchTier, b: MatchTier): MatchTier {
        val order = listOf(MatchTier.FUZZY, MatchTier.ALIAS_SEMANTIC, MatchTier.EXACT_LABEL)
        return if (order.indexOf(a) >= order.indexOf(b)) a else b
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

    private fun pickOutcome(
        ranked: List<Ranked>,
        queryKey: String,
        personal: PersonalResolutionContext?,
        learnKey: String,
    ): ResolutionOutcome {
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
            val learned = personal?.learningStore?.getWeight(learnKey, best.app.packageName) ?: 0
            val usedPref = PersonalLearningBonus.bonusPoints(learned) > 0 &&
                best.tier != MatchTier.EXACT_LABEL
            val hint = when (best.tier) {
                MatchTier.EXACT_LABEL -> "Best match: ${best.app.label}"
                MatchTier.ALIAS_SEMANTIC -> if (usedPref) "Preferred app: ${best.app.label}" else "Best match: ${best.app.label}"
                MatchTier.FUZZY -> if (usedPref) "Preferred app: ${best.app.label}" else "Close match: ${best.app.label}"
            }
            return ResolutionOutcome.SingleLaunch(
                best.app,
                hint,
                usedLearnedPreference = usedPref,
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
            subtitle = "Tap to choose · For \"$queryKey\"",
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
        val usedPersonalAlias: Boolean = false,
        val usedLearnedPreference: Boolean = false,
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
