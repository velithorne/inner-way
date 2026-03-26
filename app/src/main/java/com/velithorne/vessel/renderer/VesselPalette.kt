package com.velithorne.vessel.renderer

import androidx.compose.ui.graphics.Color
import com.velithorne.vessel.model.VesselPaletteState
import com.velithorne.vessel.physiology.SpeciesState

/** Physiology-driven palette (Phase 5). Single place for species “material” color logic. */
object VesselPalette {

    private val shellCool = Color(0xFF8DA4B8)
    private val shellWarmStress = Color(0xFFB89A7A)
    private val tealPath = Color(0xFF4ECDC4)
    private val cyanBright = Color(0xFF5EEAD4)
    private val amberHeart = Color(0xFFE8A85C)
    private val roseCore = Color(0xFFFF6B8A)
    private val violetCortex = Color(0xFFB794F6)
    private val archiveIndigo = Color(0xFF9FA8DA)
    private val archiveDeep = Color(0xFF4A5578)
    private val gelCyan = Color(0xFF7FD6E8)
    private val thermalOrange = Color(0xFFFF8A4A)
    private val thermalCrimson = Color(0xFFFF4422)
    private val recoveryMint = Color(0xFF5FD4B8)
    private val muscSlate = Color(0xFF6B7C8C)

    fun fromSpecies(s: SpeciesState, signalStrained: Float, tuning: RenderTuning): VesselPaletteState {
        val vital = s.vitality.coerceIn(0f, 1f)
        val stress = s.stress.coerceIn(0f, 1f)
        val fever = s.fever.coerceIn(0f, 1f)
        val hunger = s.hunger.coerceIn(0f, 1f)
        val recovery = s.recovery.coerceIn(0f, 1f)
        val sleep = s.sleepPressure.coerceIn(0f, 1f)
        val signal = (s.signalArousal * tuning.signalCyanBoost).coerceIn(0f, 1.2f)
        val structural = s.structuralLoad.coerceIn(0f, 1f)

        // Stable / healthy: cool silver-blue shell; stress/fever warms shell edge
        val shellBlend = (fever * 0.55f + stress * 0.35f + (1f - vital) * 0.12f).coerceIn(0f, 1f)
        val shellBase = lerpColor(shellCool, shellWarmStress, shellBlend)
        val shellEdge = lerpColor(
            Color(0xFF6EB8D4),
            Color(0xFFD47848).copy(alpha = 0.9f),
            (fever * 0.7f + stress * 0.25f).coerceIn(0f, 1f),
        )
        val shellRimCool = lerpColor(
            Color(0xFF3D5A6B),
            Color(0xFF5A3D28).copy(alpha = 0.85f),
            shellBlend,
        )

        val hungerCoreCool = hunger * 0.45f
        val heartCore = lerpColor(
            lerpColor(amberHeart, roseCore, 0.35f + vital * 0.35f),
            Color(0xFF6A5A52),
            hungerCoreCool,
        )
        val heartRing = lerpColor(Color(0xFFFFB088), roseCore, 0.4f + stress * 0.2f)

        val accentSignal = lerpColor(tealPath, cyanBright, (signal * 0.5f).coerceIn(0f, 1f))
        val metabolicPathway = lerpColor(
            Color(0xFFD4A574).copy(alpha = 0.85f),
            roseCore.copy(alpha = 0.75f),
            fever * 0.35f + (1f - vital) * 0.15f,
        )
        val neuralPathway = lerpColor(
            tealPath.copy(alpha = 0.65f),
            cyanBright.copy(alpha = 0.85f),
            (signal * 0.55f + s.neuralActivity * 0.35f).coerceIn(0f, 1f),
        )

        val cortexNode = violetCortex
        val cortexFilament = lerpColor(
            violetCortex.copy(alpha = 0.45f),
            accentSignal.copy(alpha = 0.55f),
            s.neuralActivity * 0.4f,
        )

        val gelCoherence = (1f - s.structuralLoad * 0.25f).coerceIn(0f, 1f)
        val gelMedium = lerpColor(gelCyan.copy(alpha = 0.35f), Color(0xFF4A6B78).copy(alpha = 0.4f), 1f - gelCoherence)
        val gelGrain = lerpColor(Color(0xFF9ECED8), Color(0xFF5A7A82), stress * 0.5f)

        val archivePlate = lerpColor(archiveIndigo, archiveDeep, structural * 0.65f + stress * 0.15f)
        val archiveDeepTone = lerpColor(archiveDeep, Color(0xFF1E2438), structural * 0.5f)

        val strained = signalStrained.coerceIn(0f, 1f)
        val lungFrond = lerpColor(cyanBright, Color(0xFF7AB8D4), strained * 0.35f + (1f - vital) * 0.1f)

        val musculatureTension = lerpColor(muscSlate, accentSignal.copy(alpha = 0.55f), s.mobility * 0.25f)

        val thermalHot = lerpColor(thermalOrange, thermalCrimson, fever)
        val thermalEdge = lerpColor(
            thermalOrange.copy(alpha = 0.35f),
            thermalCrimson.copy(alpha = 0.55f),
            (fever * 0.6f + stress * 0.25f).coerceIn(0f, 1f),
        )

        val recoverySheen = lerpColor(
            Color(0xFF3A5A58).copy(alpha = 0f),
            recoveryMint.copy(alpha = 0.35f + recovery * 0.25f),
            recovery * (1f - sleep * 0.4f),
        )

        val innerShadow = lerpColor(
            Color(0xFF0D1520),
            Color(0xFF1A1018),
            fever * 0.4f,
        )

        val chamberMist = lerpColor(
            Color(0xFF6B8AA0).copy(alpha = 0.12f),
            thermalOrange.copy(alpha = 0.08f + fever * 0.12f),
            fever * 0.35f,
        )

        val sleepDim = sleep * tuning.sleepDimMax
        return VesselPaletteState(
            shellBase = applySleepDim(shellBase, sleepDim),
            shellEdge = applySleepDim(shellEdge, sleepDim * 0.8f),
            shellRimCool = applySleepDim(shellRimCool, sleepDim * 0.85f),
            innerChamberShadow = innerShadow.copy(alpha = innerShadow.alpha.coerceIn(0.5f, 1f)),
            metabolicPathway = applySleepDim(metabolicPathway, sleepDim * 0.7f),
            neuralPathway = applySleepDim(neuralPathway, sleepDim * 0.65f),
            heartCore = applySleepDim(heartCore, sleepDim * 0.5f + hunger * 0.15f),
            heartRing = applySleepDim(heartRing, sleepDim * 0.45f),
            cortexNode = applySleepDim(cortexNode, sleepDim * 0.55f),
            cortexFilament = applySleepDim(cortexFilament, sleepDim * 0.55f),
            gelMedium = applySleepDim(gelMedium, sleepDim * 0.5f),
            gelGrain = applySleepDim(gelGrain, sleepDim * 0.55f),
            archivePlate = applySleepDim(archivePlate, sleepDim * 0.35f),
            archiveDeep = applySleepDim(archiveDeepTone, sleepDim * 0.35f),
            lungFrond = applySleepDim(lungFrond, sleepDim * 0.5f),
            musculatureTension = applySleepDim(musculatureTension, sleepDim * 0.6f),
            thermalHot = thermalHot,
            thermalEdge = thermalEdge,
            recoverySheen = recoverySheen.copy(alpha = recoverySheen.alpha * (1f - sleep * 0.35f)),
            accentSignal = applySleepDim(accentSignal, sleepDim * 0.6f),
            chamberMist = chamberMist.copy(alpha = chamberMist.alpha * (0.75f + structural * 0.25f)),
        )
    }

    private fun applySleepDim(c: Color, amount: Float): Color {
        val t = amount.coerceIn(0f, 0.65f)
        val f = 1f - t
        return Color(
            red = c.red * f,
            green = c.green * f,
            blue = c.blue * f,
            alpha = c.alpha * (1f - t * 0.15f),
        )
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
