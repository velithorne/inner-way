package com.velithorne.vessel.background

import com.velithorne.vessel.branching.LineageBranch
import com.velithorne.vessel.lineage.SeedPodReturnSummary
import java.security.MessageDigest

/**
 * Lines from ambient ecology patterns + optional applied structural nudges.
 * Prefers 1–3 concrete lines; avoids empty noise and duplicate phrasing across sessions (see [ambientSummaryHash]).
 */
object ReturnSummaryComposer {

    fun compose(
        awaySeconds: Long,
        acc: AmbientGrowthAccumulator.Result,
        tuning: BackgroundTuning,
        applied: AmbientProgressionApplicator.AppliedAmbient? = null,
    ): List<String> {
        if (acc.snapshotCount == 0) return emptyList()
        val lines = mutableListOf<String>()
        val head = awayHeader(awaySeconds)

        applied?.let { ap ->
            if (ap.structuralReadinessDelta >= tuning.returnSummaryReadinessMentionThreshold) {
                val pct = (ap.structuralReadinessDelta * 100f).toInt().coerceIn(1, 100)
                lines += "Crown readiness tightened ~$pct% from stored ambient rhythms (offline accumulation)."
            }
            val br = dominantBranchFromApplied(ap)
            val maxD = ap.affinityDeltas.maxOrNull() ?: 0f
            if (br != null && maxD >= tuning.returnSummaryAffinityMentionThreshold) {
                lines += "${br.displayName} affinity drew ~${(maxD * 100f).toInt()}% from recent phone ecology."
            }
        }

        if (acc.chargingSamples >= tuning.chargingSampleStrongThreshold) {
            lines += "Charging cycles while away fed recovery and shell coherence budgets."
        }
        if (acc.cellularSamples >= tuning.cellularSampleStrongThreshold) {
            lines += "Sustained mobile connectivity strengthened lateral signal tendency."
        }
        if (acc.warmThermalSamples >= 3) {
            lines += "Thermal stress samples accumulated toward shell-oriented development."
        }
        if (acc.idleStableSamples >= acc.snapshotCount / 2 && acc.idleStableSamples >= 3) {
            lines += "Calm idle stretches improved chamber coherence accumulation."
        }
        if (acc.reserveStressSamples >= acc.snapshotCount / 3 && acc.reserveStressSamples >= 3) {
            lines += "Low-charge spells left reserve-stress shaping in the lineage record."
        }
        if (acc.archiveHeavySamples >= 3) {
            lines += "Heavy storage samples nudged archive-core pressure."
        }
        if (acc.motionHighSamples >= 3) {
            lines += "Elevated motion samples leaned morphology toward braced stability."
        }

        if (lines.isEmpty() && acc.dominantDriver != "none") {
            lines += when (acc.dominantDriver) {
                "signal_mobile" -> "Background ecology leaned on mobile connectivity patterns."
                "thermal" -> "Elevated heat samples nudged thermal adaptation."
                "charging_recovery" -> "Plugged-in intervals stabilized reserve and recovery channels."
                "idle_coherence" -> "Quiet idle periods stacked coherence toward chamber stabilization."
                "reserve_stress" -> "Low-charge samples added reserve-stress shaping."
                "archive" -> "Heavy storage use nudged archive-core pressure."
                "motion" -> "Motion-rich intervals nudged bracing tendency."
                else -> "Ambient phone ecology contributed to slow background growth."
            }
        }
        if (lines.isEmpty()) return emptyList()
        val body = lines.distinct().take(3)
        return listOf(head) + body
    }

    /** Stable fingerprint for deduplicating repeat reopen toasts. */
    fun ambientSummaryHash(lines: List<String>): String {
        val raw = lines.joinToString("|")
        val md = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return md.joinToString("") { b -> "%02x".format(b) }.take(24)
    }

    fun mergeWithExisting(
        ambient: List<String>,
        existing: SeedPodReturnSummary?,
        awaySeconds: Long,
    ): SeedPodReturnSummary? {
        fun stripAwayHeader(lines: List<String>): List<String> =
            if (lines.isNotEmpty() && lines.first().startsWith("Away")) lines.drop(1) else lines
        val ambientBody = stripAwayHeader(ambient)
        val baseBody = stripAwayHeader(existing?.lines.orEmpty())
        if (ambientBody.isEmpty() && baseBody.isEmpty()) return null
        val merged = (ambientBody + baseBody).distinct().take(5)
        val header = awayHeader(awaySeconds)
        return SeedPodReturnSummary(awaySeconds = awaySeconds, lines = listOf(header) + merged)
    }

    private fun awayHeader(awaySeconds: Long): String {
        val m = awaySeconds / 60L
        val h = awaySeconds / 3600L
        val label = when {
            h >= 2L -> "Away ~${h}h"
            h == 1L -> "Away ~1h"
            m >= 2L -> "Away ~${m}m"
            else -> "Away briefly"
        }
        return "$label — ambient ecology absorbed (not live-tick simulation)."
    }

    private fun dominantBranchFromApplied(ap: AmbientProgressionApplicator.AppliedAmbient): LineageBranch? {
        var best: LineageBranch? = null
        var bestV = 0f
        for (b in LineageBranch.entries) {
            val v = ap.affinityDeltas.getOrElse(b.ordinal) { 0f }
            if (v > bestV) {
                bestV = v
                best = b
            }
        }
        return best?.takeIf { bestV > 1e-4f }
    }
}
