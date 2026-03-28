package com.velithorne.vessel.data.prefs

import android.content.Context
import com.velithorne.vessel.growthtime.DisplayMorphState
import com.velithorne.vessel.growthtime.GrowthBudget
import com.velithorne.vessel.growthtime.GrowthClock
import com.velithorne.vessel.growthtime.TemporalGrowthStage
import com.velithorne.vessel.morphogenesis.BodyMassFieldState
import com.velithorne.vessel.morphogenesis.BuddingStructure
import com.velithorne.vessel.morphogenesis.ChamberMassModel
import com.velithorne.vessel.morphogenesis.ContourParams
import com.velithorne.vessel.morphogenesis.GrowthVisualCues
import com.velithorne.vessel.morphogenesis.SeedCore

private const val PREFS = "velithorne_growth"
private const val KEY_HAS = "has_state"
private const val KEY_LAST_WALL = "last_wall_ms"
private const val KEY_STAGE_ENTERED = "stage_entered_ms"

class GrowthStateStore(context: Context) {
    private val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(
        display: DisplayMorphState,
        budget: GrowthBudget,
        stageEnteredAtMs: Long,
    ) {
        val sb = StringBuilder()
        fun f(v: Float) { sb.append(v).append(';') }
        f(display.seedFormBlend)
        with(display.contour) {
            f(crownWidthMul); f(thoraxWidthMul); f(tailLengthMul); f(asymmetryX); f(thermalBulge)
        }
        with(display.seedCore) {
            f(coreRadius); f(seedDensity); f(reserveLuminance); f(shellCoherence); f(germinationProgress)
            f(branchLatentEnergy); f(archiveLatentMass); f(signalLatentBias); f(thermalAdaptationBias); f(latticeStress)
        }
        with(display.chamberMass) {
            f(cranialCortex); f(centralMetabolic); f(lateralSignal); f(lowerArchiveBasin); f(perimeterShell)
        }
        with(display.bodyMass) {
            f(totalOccupancy); f(cranialInfluence); f(centralInfluence); f(lateralInfluence); f(lowerInfluence)
            f(shellEnvelope); f(blendSoftness)
        }
        with(display.budding) {
            f(signalFrond); f(thermalVeilSpine); f(archiveLamella); f(neuralCrownBloom); f(reserveSac)
        }
        with(display.growthVisuals) {
            f(crownBloomIntensity); f(frondBudLengthLeft); f(frondBudLengthRight); f(lowerReservoirDepth)
            f(shellThickeningIntensity); f(archiveDensityBands); f(thermalVeilIntensity)
            f(growthFrontEdgeIntensity); f(activeAccretionPulse); f(stageVisualBias); f(chamberFillVisual)
        }
        f(display.growthFrontLead)
        p.edit()
            .putBoolean(KEY_HAS, true)
            .putString("display_blob", sb.toString())
            .putInt("temporal", display.temporalStage.ordinal)
            .putLong(KEY_LAST_WALL, display.lastWallClockMs)
            .putLong(KEY_STAGE_ENTERED, stageEnteredAtMs)
            .putFloat("b_crown", budget.crownGrowthBudget)
            .putFloat("b_frond", budget.frondGrowthBudget)
            .putFloat("b_res", budget.reservoirGrowthBudget)
            .putFloat("b_shell", budget.shellGrowthBudget)
            .putFloat("b_arch", budget.archiveGrowthBudget)
            .putFloat("b_ten", budget.tendonGrowthBudget)
            .putFloat("b_rec", budget.recoveryRepairBudget)
            .apply()
    }

    fun loadBudget(): GrowthBudget? {
        if (!p.getBoolean(KEY_HAS, false)) return null
        return GrowthBudget(
            crownGrowthBudget = p.getFloat("b_crown", 0f),
            frondGrowthBudget = p.getFloat("b_frond", 0f),
            reservoirGrowthBudget = p.getFloat("b_res", 0f),
            shellGrowthBudget = p.getFloat("b_shell", 0f),
            archiveGrowthBudget = p.getFloat("b_arch", 0f),
            tendonGrowthBudget = p.getFloat("b_ten", 0f),
            recoveryRepairBudget = p.getFloat("b_rec", 0f),
        )
    }

    fun loadDisplayOrNull(): Pair<DisplayMorphState, Long>? {
        if (!p.getBoolean(KEY_HAS, false)) return null
        val blob = p.getString("display_blob", null) ?: return null
        val parts = blob.split(';').mapNotNull { it.toFloatOrNull() }
        if (parts.size < 45) return null
        var i = 0
        fun next() = parts[i++]
        val seedBlend = next()
        val contour = ContourParams(next(), next(), next(), next(), next())
        val seedCore = SeedCore(next(), next(), next(), next(), next(), next(), next(), next(), next(), next())
        val chamber = ChamberMassModel(next(), next(), next(), next(), next())
        val body = BodyMassFieldState(next(), next(), next(), next(), next(), next(), next())
        val budding = BuddingStructure(next(), next(), next(), next(), next())
        val gv = GrowthVisualCues(next(), next(), next(), next(), next(), next(), next(), next(), next(), next(), next())
        val frontLead = next()
        val temporal = TemporalGrowthStage.entries.getOrNull(p.getInt("temporal", 0)) ?: TemporalGrowthStage.DORMANT_SEED
        val lastWall = p.getLong(KEY_LAST_WALL, GrowthClock.nowMillis())
        val stageEntered = p.getLong(KEY_STAGE_ENTERED, lastWall)
        val display = DisplayMorphState(
            seedFormBlend = seedBlend,
            contour = contour,
            seedCore = seedCore,
            chamberMass = chamber,
            bodyMass = body,
            budding = budding,
            growthVisuals = gv,
            growthFrontLead = frontLead,
            temporalStage = temporal,
            lastWallClockMs = lastWall,
        )
        return display to stageEntered
    }

    fun clear() {
        p.edit().clear().apply()
    }
}
