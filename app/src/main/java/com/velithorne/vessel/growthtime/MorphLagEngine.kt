package com.velithorne.vessel.growthtime

import com.velithorne.vessel.morphogenesis.BodyMassFieldState
import com.velithorne.vessel.morphogenesis.BuddingStructure
import com.velithorne.vessel.morphogenesis.ChamberMassModel
import com.velithorne.vessel.morphogenesis.ContourParams
import com.velithorne.vessel.morphogenesis.MorphogenesisSnapshot
import com.velithorne.vessel.morphogenesis.SeedCore
import com.velithorne.vessel.util.Smoothing

/**
 * Moves [DisplayMorphState] toward [MorphTargetState] using wall-clock dt, budgets, and rate limits.
 */
object MorphLagEngine {

    fun step(
        prev: DisplayMorphState,
        target: MorphogenesisSnapshot,
        budget: GrowthBudget,
        dtSec: Float,
        tuning: TimeTuning,
    ): DisplayMorphState {
        val t = target
        val dt = dtSec.coerceIn(0f, 120f)
        val boost = 1f + (budget.crownGrowthBudget + budget.frondGrowthBudget + budget.shellGrowthBudget) / 3f * tuning.budgetSpendToLerpBoost

        val targetBlend = MorphogenesisMapperSeedBlend.targetSeedFormBlend(t)
        var blend = Smoothing.lerp(prev.seedFormBlend, targetBlend, tuning.seedFormBlendSpeed * dt * boost)
        blend = GrowthRateLimiter.limitDeltaPerMinute(prev.seedFormBlend, blend, 0.08f, dt)

        val c = t.contour
        val pc = prev.contour
        val contour = ContourParams(
            crownWidthMul = lerp(pc.crownWidthMul, c.crownWidthMul, tuning.contourMulSpeed * dt * boost),
            thoraxWidthMul = lerp(pc.thoraxWidthMul, c.thoraxWidthMul, tuning.contourMulSpeed * dt * boost),
            tailLengthMul = GrowthRateLimiter.limitDeltaPerHour(
                pc.tailLengthMul, c.tailLengthMul, tuning.maxShellDeltaPerHour * 0.5f, dt,
            ),
            asymmetryX = lerp(pc.asymmetryX, c.asymmetryX, tuning.contourMulSpeed * 0.8f * dt),
            thermalBulge = lerp(pc.thermalBulge, c.thermalBulge, tuning.contourMulSpeed * dt),
        )

        val sc = lerpSeedCore(prev.seedCore, t.seedCore, tuning.seedCoreSpeed * dt * boost)
        val ch = lerpChamber(prev.chamberMass, t.chamberMass, tuning.chamberMassSpeed * dt * boost)
        val bm = lerpBodyMass(prev.bodyMass, t.bodyMass, tuning.chamberMassSpeed * 0.85f * dt * boost)
        val bd = lerpBudding(prev.budding, t.budding, tuning.buddingSpeed * dt * boost)

        val targetVisuals = t.growthVisuals
        val pv = prev.growthVisuals
        val gv = com.velithorne.vessel.morphogenesis.GrowthVisualCues(
            crownBloomIntensity = lerpV(pv.crownBloomIntensity, targetVisuals.crownBloomIntensity, tuning.growthVisualCueSpeed * dt * (1f + budget.crownGrowthBudget)),
            frondBudLengthLeft = GrowthRateLimiter.limitDeltaPerMinute(pv.frondBudLengthLeft, targetVisuals.frondBudLengthLeft, tuning.maxFrondVisualDeltaPerMinute, dt),
            frondBudLengthRight = GrowthRateLimiter.limitDeltaPerMinute(pv.frondBudLengthRight, targetVisuals.frondBudLengthRight, tuning.maxFrondVisualDeltaPerMinute, dt),
            lowerReservoirDepth = GrowthRateLimiter.limitDeltaPerMinute(pv.lowerReservoirDepth, targetVisuals.lowerReservoirDepth, tuning.maxReservoirDeltaPerMinute, dt),
            shellThickeningIntensity = GrowthRateLimiter.limitDeltaPerHour(pv.shellThickeningIntensity, targetVisuals.shellThickeningIntensity, tuning.maxShellDeltaPerHour, dt),
            archiveDensityBands = GrowthRateLimiter.limitDeltaPerHour(pv.archiveDensityBands, targetVisuals.archiveDensityBands, tuning.maxShellDeltaPerHour * 0.8f, dt),
            thermalVeilIntensity = lerpV(pv.thermalVeilIntensity, targetVisuals.thermalVeilIntensity, tuning.growthVisualCueSpeed * 0.9f * dt),
            growthFrontEdgeIntensity = minOf(1f, pv.growthFrontEdgeIntensity * 0.92f + targetVisuals.growthFrontEdgeIntensity * 0.25f + t.growthFront.activeIntensity * 0.15f),
            activeAccretionPulse = lerpV(pv.activeAccretionPulse, targetVisuals.activeAccretionPulse, tuning.growthVisualCueSpeed * 1.1f * dt),
            stageVisualBias = lerpV(pv.stageVisualBias, targetVisuals.stageVisualBias, tuning.growthVisualCueSpeed * 0.5f * dt),
            chamberFillVisual = lerpV(pv.chamberFillVisual, targetVisuals.chamberFillVisual, tuning.chamberMassSpeed * dt * boost),
        )

        val frontLead = (0.55f + 0.45f * t.growthFront.activeIntensity.coerceIn(0f, 1f)).coerceIn(0.35f, 1f)

        return DisplayMorphState(
            seedFormBlend = blend,
            contour = contour,
            seedCore = sc,
            chamberMass = ch,
            bodyMass = bm,
            budding = bd,
            growthVisuals = gv,
            growthFrontLead = frontLead,
            temporalStage = prev.temporalStage,
            lastWallClockMs = prev.lastWallClockMs,
        )
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t.coerceIn(0f, 1f)

    private fun lerpV(a: Float, b: Float, speed: Float): Float = Smoothing.lerp(a, b, speed.coerceIn(0f, 1f))

    private fun lerpSeedCore(p: SeedCore, t: SeedCore, speed: Float): SeedCore = SeedCore(
        coreRadius = lerpV(p.coreRadius, t.coreRadius, speed),
        seedDensity = lerpV(p.seedDensity, t.seedDensity, speed),
        reserveLuminance = lerpV(p.reserveLuminance, t.reserveLuminance, speed),
        shellCoherence = lerpV(p.shellCoherence, t.shellCoherence, speed),
        germinationProgress = lerpV(p.germinationProgress, t.germinationProgress, speed * 1.1f),
        branchLatentEnergy = lerpV(p.branchLatentEnergy, t.branchLatentEnergy, speed * 0.9f),
        archiveLatentMass = lerpV(p.archiveLatentMass, t.archiveLatentMass, speed * 0.85f),
        signalLatentBias = lerpV(p.signalLatentBias, t.signalLatentBias, speed * 0.9f),
        thermalAdaptationBias = lerpV(p.thermalAdaptationBias, t.thermalAdaptationBias, speed * 0.85f),
        latticeStress = lerpV(p.latticeStress, t.latticeStress, speed * 1.2f),
    )

    private fun lerpChamber(p: ChamberMassModel, t: ChamberMassModel, speed: Float): ChamberMassModel = ChamberMassModel(
        cranialCortex = lerpV(p.cranialCortex, t.cranialCortex, speed),
        centralMetabolic = lerpV(p.centralMetabolic, t.centralMetabolic, speed),
        lateralSignal = lerpV(p.lateralSignal, t.lateralSignal, speed),
        lowerArchiveBasin = lerpV(p.lowerArchiveBasin, t.lowerArchiveBasin, speed),
        perimeterShell = lerpV(p.perimeterShell, t.perimeterShell, speed * 0.85f),
    )

    private fun lerpBodyMass(p: BodyMassFieldState, t: BodyMassFieldState, speed: Float): BodyMassFieldState = BodyMassFieldState(
        totalOccupancy = lerpV(p.totalOccupancy, t.totalOccupancy, speed),
        cranialInfluence = lerpV(p.cranialInfluence, t.cranialInfluence, speed),
        centralInfluence = lerpV(p.centralInfluence, t.centralInfluence, speed),
        lateralInfluence = lerpV(p.lateralInfluence, t.lateralInfluence, speed),
        lowerInfluence = lerpV(p.lowerInfluence, t.lowerInfluence, speed),
        shellEnvelope = lerpV(p.shellEnvelope, t.shellEnvelope, speed * 0.8f),
        blendSoftness = lerpV(p.blendSoftness, t.blendSoftness, speed * 0.7f),
    )

    private fun lerpBudding(p: BuddingStructure, t: BuddingStructure, speed: Float): BuddingStructure = BuddingStructure(
        signalFrond = lerpV(p.signalFrond, t.signalFrond, speed),
        thermalVeilSpine = lerpV(p.thermalVeilSpine, t.thermalVeilSpine, speed * 0.85f),
        archiveLamella = lerpV(p.archiveLamella, t.archiveLamella, speed * 0.85f),
        neuralCrownBloom = lerpV(p.neuralCrownBloom, t.neuralCrownBloom, speed),
        reserveSac = lerpV(p.reserveSac, t.reserveSac, speed),
    )

    fun initialFromTarget(target: MorphogenesisSnapshot): DisplayMorphState {
        val blend = MorphogenesisMapperSeedBlend.targetSeedFormBlend(target)
        return DisplayMorphState(
            seedFormBlend = blend,
            contour = target.contour,
            seedCore = target.seedCore,
            chamberMass = target.chamberMass,
            bodyMass = target.bodyMass,
            budding = target.budding,
            growthVisuals = target.growthVisuals,
            growthFrontLead = 0.75f,
            temporalStage = StageGateEngine.mapGerminationToTemporal(target.germinationStage),
            lastWallClockMs = GrowthClock.nowMillis(),
        )
    }
}

/** Computes same seed blend formula as MorphogenesisMapper for target. */
object MorphogenesisMapperSeedBlend {
    fun targetSeedFormBlend(m: MorphogenesisSnapshot): Float {
        val growthLoad = (
            m.accumulated.archive * 0.28f +
                m.accumulated.signal * 0.24f +
                m.accumulated.neural * 0.18f +
                m.genome.shellThickness * 0.22f
            ).coerceIn(0f, 1f)
        return (0.97f - growthLoad * 0.35f).coerceIn(0.72f, 0.98f)
    }
}
