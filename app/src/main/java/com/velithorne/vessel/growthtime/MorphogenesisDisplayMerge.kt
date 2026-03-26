package com.velithorne.vessel.growthtime

import com.velithorne.vessel.morphogenesis.GrowthFront
import com.velithorne.vessel.morphogenesis.MorphogenesisSnapshot

object MorphogenesisDisplayMerge {

    /**
     * Produces a snapshot for rendering: **structural** fields from lagging [DisplayMorphState],
     * graph/pathways/tissue from **target**; growth front **leads** structure.
     */
    fun merge(target: MorphogenesisSnapshot, display: DisplayMorphState): MorphogenesisSnapshot {
        val lead = display.growthFrontLead.coerceIn(0.35f, 1f)
        val gv = display.growthVisuals
        val boosted = gv.copy(
            growthFrontEdgeIntensity = (gv.growthFrontEdgeIntensity * 0.5f + target.growthVisuals.growthFrontEdgeIntensity * 0.5f * lead).coerceIn(0f, 1f),
            activeAccretionPulse = (gv.activeAccretionPulse * 0.55f + target.growthVisuals.activeAccretionPulse * 0.45f).coerceIn(0f, 1f),
        )
        val gf = GrowthFront(
            activeIntensity = (target.growthFront.activeIntensity * 0.35f + lead * 0.65f).coerceIn(0f, 1f),
            directionX = target.growthFront.directionX,
            directionY = target.growthFront.directionY,
            primaryType = target.growthFront.primaryType,
            secondaryType = target.growthFront.secondaryType,
        )
        val staged = TemporalToGerminationStage.map(display.temporalStage)
        return target.copy(
            contour = display.contour,
            seedCore = display.seedCore,
            chamberMass = display.chamberMass,
            bodyMass = display.bodyMass,
            budding = display.budding,
            growthVisuals = boosted,
            growthFront = gf,
            germinationStage = staged,
            seedFormBlendDisplay = display.seedFormBlend,
        )
    }
}
