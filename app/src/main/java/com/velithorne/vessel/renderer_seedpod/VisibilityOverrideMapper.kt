package com.velithorne.vessel.renderer_seedpod

import com.velithorne.vessel.config.SimulationMode
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage
import com.velithorne.vessel.model.BiographyVisualState
import com.velithorne.vessel.model.BranchVisualState
import com.velithorne.vessel.model.GeneratedAnatomyState
import com.velithorne.vessel.model.GeneratedTopologyState
import com.velithorne.vessel.model.SeedTraceState
import com.velithorne.vessel.model.VisibleMorphologyState
import com.velithorne.vessel.morphogenesis_core.CanonicalLifeEra
import com.velithorne.vessel.morphogenesis_core.GrowthPressureState
import com.velithorne.vessel.morphogenesis_core.MorphNodeKind
import com.velithorne.vessel.branching.LineageBranch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Maps self-assembly + branch + biography → visible override weights and topology copy.
 */
object VisibilityOverrideMapper {

    data class VisibilityBundle(
        val visible: VisibleMorphologyState,
        val seedTrace: SeedTraceState,
        val topology: GeneratedTopologyState,
        val fallbackMode: SeedPodFallbackMode,
    )

    fun map(
        anatomy: GeneratedAnatomyState?,
        biography: BiographyVisualState,
        branch: BranchVisualState,
        stage: SeedPodGrowthStage,
        structuralProgress: Float,
        mode: SimulationMode,
        pressure: GrowthPressureState?,
    ): VisibilityBundle {
        val p = pressure ?: GrowthPressureState()
        val era = anatomy?.era ?: CanonicalLifeEra.SEED
        val field = anatomy?.tissueCenter
        val graph = anatomy?.graph
        if (anatomy == null) {
            return VisibilityBundle(
                visible = VisibleMorphologyState.neutral(era),
                seedTrace = SeedTraceState.default(),
                topology = GeneratedTopologyState(0, 0, biography.rerouteCount, biography.scars.size, p.maxComponent().name, null),
                fallbackMode = SeedPodFallbackMode.SEED_DOMINANT,
            )
        }

        val eraBase = when (era) {
            CanonicalLifeEra.SEED -> 0.06f
            CanonicalLifeEra.VEIL_STAGE -> 0.26f
            CanonicalLifeEra.CORE_ESTABLISHMENT -> 0.52f
            CanonicalLifeEra.BRANCHING_THRESHOLD -> 0.72f
            CanonicalLifeEra.ADULTHOOD -> 0.86f
        }
        val devBoost = if (mode == SimulationMode.DEV_SIMULATION) 0.14f else 0f
        val stageBoost = (stage.ordinal / 10f).coerceIn(0f, 0.12f)
        val matBoost = structuralProgress.coerceIn(0f, 1f) * 0.1f
        val branchBoost = branch.branchReadiness * 0.14f + branch.visualExpressionMagnitude * 0.12f
        val bioBoost = min(0.22f, biography.scars.size * 0.045f + biography.rerouteCount * 0.035f)

        var genInf = (eraBase + devBoost + stageBoost + matBoost + branchBoost + bioBoost).coerceIn(0f, 1f)
        genInf = applyBranchTopologyBias(genInf, branch.leadingBranch)

        val fallback = (1f - genInf).coerceIn(0f, 1f)

        val crownNode = graph?.nodes?.firstOrNull { it.kind == MorphNodeKind.CROWN_CHAMBER }
        val reserveNode = graph?.nodes?.firstOrNull { it.kind == MorphNodeKind.RESERVE_BASIN_LOCUS }
        val frondNodes = graph?.nodes?.filter { it.kind == MorphNodeKind.FROND_ROOT }.orEmpty()
        val coreNode = graph?.nodes?.firstOrNull { it.kind == MorphNodeKind.CORE_KNOT }

        val crownNx = crownNode?.nx ?: (0.48f + p.signal * 0.04f)
        val crownNy = crownNode?.ny ?: (0.22f - p.signal * 0.03f)
        val crownR = (0.08f + (crownNode?.strength ?: 0.5f) * 0.06f + (field?.density?.let { it * 0.04f } ?: 0f)).coerceIn(0.06f, 0.2f)

        val resNx = reserveNode?.nx ?: (0.5f + (1f - p.reserve) * 0.06f)
        val resNy = reserveNode?.ny ?: (0.72f + p.starvation * 0.04f)
        val resDepth = (0.65f + p.reserve * 0.25f + (reserveNode?.strength ?: 0.4f) * 0.35f).coerceIn(0.5f, 1.35f)

        val (fLeft, fRight) = if (frondNodes.size >= 2) {
            val sorted = frondNodes.sortedBy { it.nx }
            sorted[0].nx to sorted.last().nx
        } else if (frondNodes.size == 1) {
            val f = frondNodes[0].nx
            val spread = 0.12f + p.signal * 0.1f + branch.lateralAsymmetryBoost
            (f - spread * 0.5f).coerceIn(0.12f, 0.55f) to (f + spread * 0.5f).coerceIn(0.45f, 0.88f)
        } else {
            val base = 0.5f + p.signal * 0.08f
            val asym = 0.08f + abs(branch.lateralAsymmetryBoost) + p.motion * 0.06f
            (base - 0.18f - asym).coerceIn(0.1f, 0.48f) to (base + 0.18f + asym * 0.7f).coerceIn(0.52f, 0.9f)
        }

        val curvature = (branch.lateralFrondMul * 0.35f + p.signal * 0.45f + 0.7f).coerceIn(0.65f, 1.55f)
        val density = (branch.lateralFrondMul * 0.4f + (field?.density?.let { it * 0.35f } ?: 0.2f) + 0.75f).coerceIn(0.55f, 1.4f)
        val frondAsym = (branch.lateralAsymmetryBoost * 2f + p.motion * 0.25f + (fRight - fLeft - 0.5f).let { abs(it) }).coerceIn(0f, 1f)

        val th = field?.thermalTension ?: p.thermal
        val arch = field?.archiveBurden ?: p.archive
        val shellUpper = (0.35f + th * 0.45f + branch.shellBandMul * 0.15f).coerceIn(0.2f, 1f)
        val shellLower = (0.4f + p.reserve * 0.35f + branch.reserveBulbMul * 0.2f).coerceIn(0.25f, 1f)
        val shellLeft = (0.38f + arch * 0.35f + branch.innerMassMul * 0.12f).coerceIn(0.2f, 1f)
        val shellRight = (0.38f + p.signal * 0.4f + branch.lateralFrondMul * 0.15f).coerceIn(0.2f, 1f)

        val asymScore = (
            frondAsym * 0.35f +
                abs(crownNx - 0.5f) * 1.2f +
                abs(resNx - 0.5f) * 0.8f +
                biography.scars.size * 0.04f +
                biography.rerouteCount * 0.03f
            ).coerceIn(0f, 1f)

        val driver = when (branch.leadingBranch) {
            LineageBranch.THERMAL_SHELL -> "thermal shell plating"
            LineageBranch.SIGNAL_FROND -> "lateral signal routing"
            LineageBranch.CROWN_NEURAL -> "crown neural chamber"
            LineageBranch.RESERVE_BASIN -> "reserve basin mass"
            LineageBranch.ARCHIVE_CORE -> "archive core density"
            LineageBranch.MOTION_BRACED -> "motion bracing tension"
            LineageBranch.BALANCED -> "balanced multi-channel pressure"
        }

        val lines = buildTopologyLines(
            era = era,
            genInf = genInf,
            branch = branch.leadingBranch,
            shellUpper = shellUpper,
            resDepth = resDepth,
            asymScore = asymScore,
            biography = biography,
        )

        val visible = VisibleMorphologyState(
            fallbackSeedInfluence = fallback,
            generatedTopologyInfluence = genInf,
            era = era,
            dominantContourDriver = driver,
            visibleAsymmetryScore = asymScore,
            crownChamberNx = crownNx,
            crownChamberNy = crownNy,
            crownChamberRadiusMul = crownR * 10f,
            reserveBasinNx = resNx,
            reserveBasinNy = resNy,
            reserveBasinDepthMul = resDepth,
            frondRootLeftNx = fLeft,
            frondRootRightNx = fRight,
            frondCurvatureMul = curvature,
            frondDensityMul = density,
            frondAsymmetryMul = frondAsym,
            shellUpperPlate = shellUpper,
            shellLowerPlate = shellLower,
            shellLeftWing = shellLeft,
            shellRightWing = shellRight,
            topologySummaryLines = lines,
        )

        val traceAlpha = (fallback * 0.95f + (1f - genInf) * 0.4f).coerceIn(0.08f, 0.92f)
        val seam = (coreNode?.nx ?: 0.5f) * 0.4f - (coreNode?.ny ?: 0.45f) * 0.35f
        val seedTrace = SeedTraceState(
            traceAlpha = traceAlpha * (1f - genInf * 0.65f).coerceIn(0.15f, 1f),
            seamAngleRad = seam,
            coreKnotScaleMul = (0.42f + (1f - genInf) * 0.4f).coerceIn(0.35f, 0.85f),
            traceOffsetNx = (coreNode?.nx ?: 0.5f) - 0.5f,
            traceOffsetNy = (coreNode?.ny ?: 0.45f) - 0.45f,
        )

        val topo = GeneratedTopologyState(
            nodeCount = graph?.nodes?.size ?: 0,
            edgeCount = graph?.edges?.size ?: 0,
            rerouteCount = biography.rerouteCount,
            scarCount = biography.scars.size,
            maxPressureChannelLabel = p.maxComponent().name,
            graph = graph,
        )

        val fbMode = when {
            genInf < 0.22f -> SeedPodFallbackMode.SEED_DOMINANT
            genInf < 0.45f -> SeedPodFallbackMode.BLEND_EMERGING
            genInf < 0.72f -> SeedPodFallbackMode.GENERATED_PRIMARY
            else -> SeedPodFallbackMode.GENERATED_OVERRIDE
        }

        return VisibilityBundle(visible, seedTrace, topo, fbMode)
    }

    private fun applyBranchTopologyBias(genInf: Float, lead: LineageBranch): Float {
        val nudge = when (lead) {
            LineageBranch.SIGNAL_FROND -> 0.06f
            LineageBranch.CROWN_NEURAL -> 0.05f
            LineageBranch.RESERVE_BASIN -> 0.05f
            LineageBranch.ARCHIVE_CORE -> 0.04f
            LineageBranch.MOTION_BRACED -> 0.05f
            LineageBranch.THERMAL_SHELL -> 0.05f
            LineageBranch.BALANCED -> 0f
        }
        return (genInf + nudge).coerceIn(0f, 1f)
    }

    private fun buildTopologyLines(
        era: CanonicalLifeEra,
        genInf: Float,
        branch: LineageBranch,
        shellUpper: Float,
        resDepth: Float,
        asymScore: Float,
        biography: BiographyVisualState,
    ): List<String> {
        val out = mutableListOf<String>()
        when {
            genInf < 0.22f -> out += "Seed trace dominant — topology forming"
            genInf < 0.45f -> out += "Generated chambers emerging through veil"
            genInf < 0.72f -> out += "Self-assembly contour shaping shell and chambers"
            else -> out += "Generated anatomy dominates — seed visible as trace only"
        }
        when (branch) {
            LineageBranch.CROWN_NEURAL -> out += "Crown-expanded upper chamber (neural bias)"
            LineageBranch.SIGNAL_FROND -> out += "Lateral signal routing asymmetry"
            LineageBranch.RESERVE_BASIN -> out += "Lower reserve basin deepened"
            LineageBranch.ARCHIVE_CORE -> out += "Dense central archive plating"
            LineageBranch.MOTION_BRACED -> out += "Motion-braced tension pattern"
            LineageBranch.THERMAL_SHELL -> out += "Thermal shell bands on perimeter"
            LineageBranch.BALANCED -> out += "Balanced ecology — moderate topology spread"
        }
        if (shellUpper > 0.62f) out += "Upper thermal veil / shell plates emphasized"
        if (resDepth > 0.95f) out += "Reserve basin depth pronounced"
        if (asymScore > 0.35f) out += "Visible asymmetry from history"
        if (biography.rerouteCount > 0) out += "Reroute seams preserved (${biography.rerouteCount})"
        if (biography.scars.isNotEmpty()) out += "Growth scars on ${biography.scars.size} loci"
        return out.distinct().take(6)
    }
}
