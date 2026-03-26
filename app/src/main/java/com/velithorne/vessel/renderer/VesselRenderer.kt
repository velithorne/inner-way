package com.velithorne.vessel.renderer

import androidx.compose.ui.graphics.Color
import com.velithorne.vessel.model.GrowthStageVisualState
import com.velithorne.vessel.model.SeedVisualState
import com.velithorne.vessel.model.VesselMaterialState
import com.velithorne.vessel.model.VesselPaletteState
import com.velithorne.vessel.morphogenesis.GrowthExplainer
import com.velithorne.vessel.morphogenesis.MorphogenesisSnapshot
import com.velithorne.vessel.morphogenesis.StructuralGraph
import com.velithorne.vessel.physiology.OrganState
import com.velithorne.vessel.physiology.OrganType
import com.velithorne.vessel.physiology.PhysiologySnapshot
import com.velithorne.vessel.physiology.SpeciesState
import com.velithorne.vessel.telemetry.NetworkTransport

/**
 * Physiology + morphogenesis → [VesselSceneState].
 */
class VesselRenderer(
    private val layout: VesselLayout = VesselLayout(),
    private val tuning: RenderTuning = RenderTuning(),
) {

    fun map(snapshot: PhysiologySnapshot, morphogenesis: MorphogenesisSnapshot): VesselSceneState {
        val s = snapshot.species
        val telem = snapshot.telemetry
        val organs = snapshot.organs.associateBy { it.organType }
        val structuralGraph: StructuralGraph = morphogenesis.graph

        val vitalityGlow = (s.vitality * tuning.vitalityGlowScale).coerceIn(0f, 1.4f)
        val stressTint = s.stress.coerceIn(0f, 1f)
        val feverIntensity = s.fever.coerceIn(0f, 1f)
        val recoveryGlow = s.recovery.coerceIn(0f, 1f)
        val mobilitySway = s.mobility.coerceIn(0f, 1f)
        val signalBrightness = (s.signalArousal * tuning.signalCyanBoost).coerceIn(0f, 1.2f)
        val sleepDim = (s.sleepPressure * tuning.sleepDimMax).coerceIn(0f, tuning.sleepDimMax)
        val structuralMass = (s.structuralLoad * 0.85f + stressTint * 0.15f).coerceIn(0f, 1f)

        val basePulse = tuning.pulseAmplitudeVitalityScale * (0.55f + s.vitality * 0.45f)
        val stressPulseBoost = s.stress * tuning.pulseAmplitudeStressBoost
        val hungerPulseDamp = 1f - s.hunger * 0.35f
        val bodyPulseAmplitude = (basePulse + stressPulseBoost) * hungerPulseDamp

        val breath = tuning.breathAmplitudeRespirationScale * (0.45f + s.respiration * 0.9f)

        val shellMul = 0.98f + morphogenesis.genome.shellThickness * 0.04f
        val bodyScale = (0.96f + s.vitality * 0.06f + s.recovery * 0.02f) * tuning.bodyProfileScale * shellMul

        val neuralDrive = s.neuralActivity.coerceIn(0f, 1f)
        val vascularPulse = (s.vitality * 0.35f + neuralDrive * 0.45f + s.signalArousal * 0.35f)
            .coerceIn(0f, 1f)

        val thermalAgitation = (
            feverIntensity * 0.8f +
                (organs[OrganType.THERMAL_MEMBRANE]?.inflammation ?: 0f) * 0.9f
            ).coerceIn(0f, 1f)

        val particleDensity = (
            0.25f + s.respiration * 0.2f + neuralDrive * 0.25f + feverIntensity * 0.15f +
                s.signalArousal * 0.2f + structuralMass * 0.12f
            ).coerceIn(0f, 1f) * tuning.fogDensityScale

        val fogDensity = (0.2f + structuralMass * 0.35f + feverIntensity * 0.25f).coerceIn(0f, 1f)

        val signalStrained = when {
            telem.networkConnected == false -> 0.55f
            telem.networkType == NetworkTransport.CELLULAR && telem.networkMetered == true -> 0.45f
            else -> 0.12f
        }

        val hungerDim = s.hunger.coerceIn(0f, 1f)
        val stressShiver = (s.stress * tuning.stressShiverDegrees / 8f).coerceIn(0f, 1f)

        val organVisuals = buildOrganVisuals(organs, s, feverIntensity, structuralGraph, morphogenesis)

        val seedVisual = SeedVisualState(
            coreRadiusNorm = morphogenesis.seedCore.coreRadius,
            seedDensity = morphogenesis.seedCore.seedDensity,
            reserveLuminance = morphogenesis.seedCore.reserveLuminance,
            shellCoherence = morphogenesis.seedCore.shellCoherence,
            germinationProgress = morphogenesis.seedCore.germinationProgress,
            branchLatentEnergy = morphogenesis.seedCore.branchLatentEnergy,
            latticeStress = morphogenesis.seedCore.latticeStress,
        )
        val growthStageVisual = GrowthStageVisualState(
            stage = morphogenesis.germinationStage,
            stageLabel = GrowthExplainer.stageDisplayName(morphogenesis.germinationStage),
            growthFrontIntensity = morphogenesis.growthFront.activeIntensity,
            growthFrontDirX = morphogenesis.growthFront.directionX,
            growthFrontDirY = morphogenesis.growthFront.directionY,
            primaryFront = morphogenesis.growthFront.primaryType,
            secondaryFront = morphogenesis.growthFront.secondaryType,
        )

        val thermalTint = lerpColor(
            Color(0xFF1A2A28),
            Color(0xFFFF6B35).copy(alpha = 0.55f),
            feverIntensity * tuning.feverThermalTint,
        )
        val accentBias = lerpColor(
            Color(0xFF4ECDC4),
            Color(0xFF5EEAD4),
            (signalBrightness * 0.4f).coerceIn(0f, 1f),
        )
        val palette: VesselPaletteState = VesselPalette.fromSpecies(s, signalStrained, tuning)
        val material: VesselMaterialState = VesselMaterialSystem.derive(s, tuning)
        val generated = MorphogenesisMapper.toGeneratedParams(morphogenesis)

        return VesselSceneState(
            timestampMillis = snapshot.timestampMillis,
            bodyScale = bodyScale,
            bodyPulseAmplitude = bodyPulseAmplitude,
            bodyBreathAmplitude = breath,
            vitalityGlow = vitalityGlow,
            stressTint = stressTint,
            feverIntensity = feverIntensity,
            recoveryGlow = recoveryGlow,
            mobilitySway = mobilitySway,
            signalBrightness = signalBrightness,
            sleepDimming = sleepDim,
            structuralMass = structuralMass,
            stressShiver = stressShiver,
            neuralDrive = neuralDrive,
            respirationDrive = s.respiration,
            hungerDim = hungerDim,
            thermalAgitation = thermalAgitation,
            particleDensity = particleDensity,
            fogDensity = fogDensity,
            signalStrained = signalStrained,
            organVisuals = organVisuals,
            vascularPulse = vascularPulse,
            statusLine = statusSentence(s),
            healthLabel = s.healthLabel,
            vitalityDisplay = s.vitality,
            feverLabel = feverWord(s.fever),
            hungerLabel = hungerWord(s.hunger),
            accentBias = accentBias,
            thermalTint = thermalTint,
            palette = palette,
            material = material,
            generated = generated,
            structuralGraph = structuralGraph,
            seedVisual = seedVisual,
            growthStageVisual = growthStageVisual,
            seedPresentationActive = true,
        )
    }

    private fun buildOrganVisuals(
        organs: Map<OrganType, com.velithorne.vessel.physiology.OrganState>,
        species: SpeciesState,
        globalFever: Float,
        graph: com.velithorne.vessel.morphogenesis.StructuralGraph,
        morph: MorphogenesisSnapshot,
    ): List<OrganVisualModel> {
        return OrganType.values().flatMap { type ->
            val anchors = anchorsForType(type, graph)
            anchors.map { anchor ->
                mapOrganAt(type, organs[type], species, globalFever, anchor, morph)
            }
        }
    }

    private fun anchorsForType(type: OrganType, graph: com.velithorne.vessel.morphogenesis.StructuralGraph): List<VesselLayout.OrganAnchor> {
        val nodes = graph.nodes.filter { it.organType == type && it.kind == com.velithorne.vessel.morphogenesis.StructuralNodeKind.ORGAN }
            .sortedBy { it.id }
        if (nodes.isNotEmpty()) {
            return nodes.map { n ->
                VesselLayout.OrganAnchor(n.nx, n.ny, n.influenceRadius.coerceIn(0.03f, 0.55f))
            }
        }
        return when (type) {
            OrganType.SIGNAL_LUNGS -> {
                val left = layout.anchors[OrganType.SIGNAL_LUNGS]
                    ?: VesselLayout.OrganAnchor(0.35f, 0.48f, 0.065f)
                listOf(left, left.copy(x = 1f - left.x))
            }
            else -> listOfNotNull(layout.anchors[type] ?: fallbackAnchor(type))
        }
    }

    private fun fallbackAnchor(type: OrganType) = when (type) {
        OrganType.METABOLIC_HEART -> VesselLayout.OrganAnchor(0.5f, 0.62f, 0.07f)
        OrganType.CORTEX_CLUSTER -> VesselLayout.OrganAnchor(0.5f, 0.32f, 0.09f)
        OrganType.NEURAL_GEL -> VesselLayout.OrganAnchor(0.5f, 0.38f, 0.12f)
        OrganType.ARCHIVE_VAULT -> VesselLayout.OrganAnchor(0.5f, 0.76f, 0.11f)
        OrganType.SIGNAL_LUNGS -> VesselLayout.OrganAnchor(0.35f, 0.48f, 0.065f)
        OrganType.VESTIBULAR_MUSCULATURE -> VesselLayout.OrganAnchor(0.5f, 0.52f, 0.14f)
        OrganType.THERMAL_MEMBRANE -> VesselLayout.OrganAnchor(0.5f, 0.5f, 0.5f)
    }

    private fun mapOrganAt(
        type: OrganType,
        state: OrganState?,
        species: SpeciesState,
        globalFever: Float,
        anchor: VesselLayout.OrganAnchor?,
        morph: MorphogenesisSnapshot,
    ): OrganVisualModel {
        val a = anchor ?: return defaultOrgan(type)
        val embed = morph.organEmbedding.forType(type)
        val h = state?.health ?: 0.55f
        val load = state?.load ?: 0.35f
        val activity = state?.activity ?: 0.4f
        val infl = state?.inflammation ?: 0.2f
        val reserve = state?.reserve ?: 0.5f

        val scale = tuning.organScaleGlobal
        val baseR = a.radius * scale

        return when (type) {
            OrganType.METABOLIC_HEART -> OrganVisualModel(
                type, a.x, a.y, baseR,
                glowIntensity = (species.vitality * 0.55f + reserve * 0.45f + activity * 0.25f).coerceIn(0f, 1.2f),
                pulseCoupling = (0.45f + load * 0.6f + activity * 0.35f).coerceIn(0f, 1.3f),
                flickerIntensity = 0.08f,
                strain = load,
                densityLines = species.hunger * 0.5f,
                thermalCoupling = globalFever * 0.35f,
                reserveLevel = reserve,
                tissueEmbedding = embed,
            )
            OrganType.CORTEX_CLUSTER -> OrganVisualModel(
                type, a.x, a.y, baseR * 1.05f,
                glowIntensity = (species.neuralActivity * 0.7f + h * 0.35f).coerceIn(0f, 1.2f),
                pulseCoupling = 0.35f,
                flickerIntensity = (species.neuralActivity * 0.85f + species.stress * 0.35f).coerceIn(0f, 1f),
                strain = species.stress,
                densityLines = species.neuralActivity * 0.4f,
                thermalCoupling = globalFever * 0.2f,
                reserveLevel = h,
                tissueEmbedding = embed,
            )
            OrganType.NEURAL_GEL -> OrganVisualModel(
                type, a.x, a.y, baseR * 1.35f,
                glowIntensity = (h * 0.5f + (1f - load) * 0.35f).coerceIn(0f, 0.9f),
                pulseCoupling = 0.25f,
                flickerIntensity = ((1f - h) * 0.5f + species.neuralActivity * 0.3f).coerceIn(0f, 1f),
                strain = load,
                densityLines = (1f - h) * 0.7f + species.structuralLoad * 0.15f,
                thermalCoupling = 0.15f,
                reserveLevel = reserve,
                tissueEmbedding = embed,
            )
            OrganType.ARCHIVE_VAULT -> OrganVisualModel(
                type, a.x, a.y, baseR * 1.15f,
                glowIntensity = (species.structuralLoad * 0.5f + load * 0.35f).coerceIn(0f, 1f),
                pulseCoupling = 0.2f,
                flickerIntensity = 0.12f,
                strain = load,
                densityLines = (species.structuralLoad * 0.9f + (1f - reserve) * 0.3f).coerceIn(0f, 1f),
                thermalCoupling = globalFever * 0.25f,
                reserveLevel = reserve,
                tissueEmbedding = embed,
            )
            OrganType.SIGNAL_LUNGS -> OrganVisualModel(
                type, a.x, a.y, baseR,
                glowIntensity = (species.respiration * 0.45f + species.signalArousal * 0.55f).coerceIn(0f, 1.1f),
                pulseCoupling = (0.4f + species.respiration * 0.5f).coerceIn(0f, 1.2f),
                flickerIntensity = species.signalArousal * 0.45f,
                strain = 1f - species.respiration,
                densityLines = 0.2f,
                thermalCoupling = 0.1f,
                reserveLevel = activity,
                tissueEmbedding = embed,
            )
            OrganType.VESTIBULAR_MUSCULATURE -> OrganVisualModel(
                type, a.x, a.y, baseR * 1.4f,
                glowIntensity = (species.mobility * 0.4f + h * 0.3f).coerceIn(0f, 0.85f),
                pulseCoupling = 0.3f,
                flickerIntensity = species.mobility * 0.25f,
                strain = load.coerceIn(0f, 1f),
                densityLines = species.mobility * 0.65f,
                thermalCoupling = globalFever * 0.15f,
                reserveLevel = h,
                tissueEmbedding = embed,
            )
            OrganType.THERMAL_MEMBRANE -> OrganVisualModel(
                type, a.x, a.y, baseR * 2.2f,
                glowIntensity = (globalFever * 0.7f + infl * 0.45f + species.stress * 0.25f).coerceIn(0f, 1.1f),
                pulseCoupling = 0.5f + globalFever * 0.3f,
                flickerIntensity = globalFever * 0.55f,
                strain = infl,
                densityLines = 0.15f,
                thermalCoupling = 1f,
                reserveLevel = 1f - globalFever,
                tissueEmbedding = embed,
            )
        }
    }

    private fun defaultOrgan(type: OrganType) = OrganVisualModel(
        type = type,
        anchorX = 0.5f,
        anchorY = 0.5f,
        baseRadius = 0.05f,
        glowIntensity = 0f,
        pulseCoupling = 0.5f,
        flickerIntensity = 0f,
        strain = 0f,
        densityLines = 0f,
        thermalCoupling = 0f,
        reserveLevel = 0.5f,
        tissueEmbedding = 0.5f,
    )

    private fun feverWord(f: Float): String = when {
        f > 0.65f -> "High"
        f > 0.38f -> "Elevated"
        f > 0.15f -> "Mild"
        else -> "Norm"
    }

    private fun hungerWord(h: Float): String = when {
        h > 0.7f -> "Severe"
        h > 0.45f -> "Moderate"
        h > 0.22f -> "Light"
        else -> "Satiated"
    }

    private fun statusSentence(s: SpeciesState): String {
        val parts = mutableListOf<String>()
        if (s.fever > 0.35f) parts += "Thermal compensation active."
        if (s.hunger > 0.5f) parts += "Core reserve drawing down."
        if (s.structuralLoad > 0.55f) parts += "Archive strata compressing."
        if (s.recovery > 0.55f && s.stress < 0.5f) parts += "Regenerative tone elevated."
        if (s.neuralActivity > 0.6f) parts += "Bioelectric traffic dense."
        if (parts.isEmpty()) {
            parts += if (s.vitality > 0.55f) "Specimen within nominal envelope." else "Homeostasis adapting."
        }
        val core = if (s.vitality > 0.48f) "Core reserve stable." else "Core reserve soft."
        return (parts.take(2) + core).distinct().joinToString(" ")
    }

    private fun lerpColor(a: Color, b: Color, t: Float): Color {
        val tt = t.coerceIn(0f, 1f)
        return Color(
            red = a.red + (b.red - a.red) * tt,
            green = a.green + (b.green - a.green) * tt,
            blue = a.blue + (b.blue - a.blue) * tt,
            alpha = a.alpha + (b.alpha - a.alpha) * tt,
        )
    }
}
