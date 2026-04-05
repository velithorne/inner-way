package com.velithorne.innerway.mind

import android.content.Context
import androidx.core.content.edit
import com.velithorne.innerway.memory.MemoryEntity
import com.velithorne.innerway.perception.EnvironmentalContext
import org.json.JSONObject
import kotlin.math.exp
import kotlin.math.min

/**
 * Slow accumulation of growth personality from memory logs + session signals.
 * Persisted across app restarts.
 */
class GrowthImprintEngine(
    context: Context,
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private var model: GrowthImprintModel = load()

    fun current(): GrowthImprintModel = model.clamped()

    /**
     * @param dtSeconds wall-clock step for session nudges (typically ~1s from main loop).
     */
    fun tick(
        dtSeconds: Float,
        memories: List<MemoryEntity>,
        environment: EnvironmentalContext,
        internalState: InternalState,
        hints: SomaticHints,
    ): GrowthImprintModel {
        val dt = dtSeconds.coerceIn(0f, 5f)
        val memTarget = targetFromMemories(memories)
        val session = sessionNudge(dt, environment, internalState, hints)

        fun blend(field: (GrowthImprintModel) -> Float, memT: Float, sess: Float): Float {
            val cur = field(model)
            val target = (memT * 0.65f + sess * 0.35f).coerceIn(0f, 1f)
            // Slow approach — no wild jumps
            val alpha = (0.018f * dt).coerceIn(0f, 0.08f)
            return cur + (target - cur) * alpha
        }

        model = GrowthImprintModel(
            stressLoad = blend({ it.stressLoad }, memTarget.stressLoad, session.stressLoad),
            calmReserve = blend({ it.calmReserve }, memTarget.calmReserve, session.calmReserve),
            recoveryStrength = blend({ it.recoveryStrength }, memTarget.recoveryStrength, session.recoveryStrength),
            disturbanceBias = blend({ it.disturbanceBias }, memTarget.disturbanceBias, session.disturbanceBias),
            stillnessAffinity = blend({ it.stillnessAffinity }, memTarget.stillnessAffinity, session.stillnessAffinity),
            chargeTrust = blend({ it.chargeTrust }, memTarget.chargeTrust, session.chargeTrust),
            asymmetryBias = blend({ it.asymmetryBias }, memTarget.asymmetryBias, session.asymmetryBias),
            branchingConfidence = blend({ it.branchingConfidence }, memTarget.branchingConfidence, session.branchingConfidence),
            plateFormationBias = blend({ it.plateFormationBias }, memTarget.plateFormationBias, session.plateFormationBias),
            contractionMemory = blend({ it.contractionMemory }, memTarget.contractionMemory, session.contractionMemory),
        ).clamped()

        // Gentle drift toward neutral when history is sparse (avoid stuck extremes)
        model = softenExtremes(model, dt)
        persist()
        return model
    }

    private data class Target(
        val stressLoad: Float,
        val calmReserve: Float,
        val recoveryStrength: Float,
        val disturbanceBias: Float,
        val stillnessAffinity: Float,
        val chargeTrust: Float,
        val asymmetryBias: Float,
        val branchingConfidence: Float,
        val plateFormationBias: Float,
        val contractionMemory: Float,
    )

    private fun targetFromMemories(memories: List<MemoryEntity>): Target {
        if (memories.isEmpty()) {
            return Target(0.35f, 0.4f, 0.35f, 0.25f, 0.3f, 0.35f, 0.25f, 0.4f, 0.35f, 0.3f)
        }
        val now = System.currentTimeMillis()
        var wSum = 1e-6f
        var stressW = 0f
        var calmW = 0f
        var recoverW = 0f
        var disturbW = 0f
        var careW = 0f
        var stressEvents = 0f

        for (m in memories) {
            val ageSec = ((now - m.timestampMillis) / 1000f).coerceAtLeast(0f)
            // Half-life ~3 days in weight
            val w = exp(-ageSec / (3f * 24f * 3600f)).toFloat().coerceIn(0.02f, 1f)
            wSum += w
            val st = parseState(m.interpretedState)
            when (st) {
                InternalState.STRESSED -> stressW += w
                InternalState.CALM, InternalState.RESTING -> calmW += w
                InternalState.RECOVERING -> recoverW += w
                InternalState.DISTURBED, InternalState.DEFENSIVE -> disturbW += w
                InternalState.CURIOUS, InternalState.ALERT -> {
                    disturbW += w * 0.35f
                    calmW += w * 0.2f
                }
                else -> {}
            }
            if (m.stressIncident) stressEvents += w
            if (m.careIncident) careW += w
            if (m.bodyConditionsSummary.contains("charging", ignoreCase = true) &&
                st == InternalState.RECOVERING
            ) {
                careW += w * 0.5f
            }
        }

        val stressT = ((stressW + stressEvents * 0.4f) / wSum).coerceIn(0f, 1f)
        val calmT = (calmW / wSum).coerceIn(0f, 1f)
        val recT = (recoverW / wSum).coerceIn(0f, 1f)
        val distT = (disturbW / wSum + stressEvents * 0.15f).coerceIn(0f, 1f)
        val chargeT = (careW / wSum).coerceIn(0f, 1f)

        return Target(
            stressLoad = stressT * 0.85f + 0.08f,
            calmReserve = calmT * 0.9f + 0.05f,
            recoveryStrength = recT * 0.85f + 0.1f,
            disturbanceBias = distT * 0.9f,
            stillnessAffinity = calmT * 0.45f + 0.25f,
            chargeTrust = chargeT * 0.8f + 0.12f,
            asymmetryBias = (stressT * 0.5f + distT * 0.35f).coerceIn(0f, 1f),
            branchingConfidence = (calmT * 0.35f + distT * 0.4f + recT * 0.25f).coerceIn(0f, 1f),
            plateFormationBias = (calmT * 0.55f + recT * 0.3f).coerceIn(0f, 1f),
            contractionMemory = (stressT * 0.7f + stressEvents * 0.1f).coerceIn(0f, 1f),
        )
    }

    private fun sessionNudge(
        dt: Float,
        env: EnvironmentalContext,
        state: InternalState,
        hints: SomaticHints,
    ): Target {
        val m = model
        var stress = m.stressLoad
        var calm = m.calmReserve
        var rec = m.recoveryStrength
        var dist = m.disturbanceBias
        var still = m.stillnessAffinity
        var charge = m.chargeTrust
        var asym = m.asymmetryBias
        var branch = m.branchingConfidence
        var plate = m.plateFormationBias
        var contract = m.contractionMemory

        val micro = 0.012f * dt
        when (state) {
            InternalState.STRESSED -> stress = min(1f, stress + micro * 1.2f)
            InternalState.CALM -> calm = min(1f, calm + micro * 1.1f)
            InternalState.RECOVERING -> rec = min(1f, rec + micro * 1.3f)
            InternalState.DISTURBED, InternalState.DEFENSIVE -> dist = min(1f, dist + micro * 1f)
            InternalState.RESTING, InternalState.DORMANT -> still = min(1f, still + micro * 0.9f)
            else -> {}
        }
        if (hints.stillnessDurationSeconds > 45f) {
            still = min(1f, still + micro * 0.6f)
        }
        if (hints.disturbanceScore > 0.35f) {
            dist = min(1f, dist + micro * 0.8f)
        }
        if (env.charging && state == InternalState.RECOVERING) {
            charge = min(1f, charge + micro * 1.4f)
            rec = min(1f, rec + micro * 0.4f)
        }
        if (state == InternalState.CURIOUS) {
            branch = min(1f, branch + micro * 0.7f)
            asym = min(1f, asym + micro * 0.35f)
        }
        if (state == InternalState.STRESSED || state == InternalState.DEFENSIVE) {
            contract = min(1f, contract + micro * 0.9f)
        }

        return Target(stress, calm, rec, dist, still, charge, asym, branch, plate, contract)
    }

    private fun softenExtremes(m: GrowthImprintModel, dt: Float): GrowthImprintModel {
        val r = 0.004f * dt
        fun pull(x: Float, center: Float) = x + (center - x) * r
        return GrowthImprintModel(
            stressLoad = pull(m.stressLoad, 0.42f),
            calmReserve = pull(m.calmReserve, 0.45f),
            recoveryStrength = pull(m.recoveryStrength, 0.42f),
            disturbanceBias = pull(m.disturbanceBias, 0.35f),
            stillnessAffinity = pull(m.stillnessAffinity, 0.38f),
            chargeTrust = pull(m.chargeTrust, 0.4f),
            asymmetryBias = pull(m.asymmetryBias, 0.35f),
            branchingConfidence = pull(m.branchingConfidence, 0.42f),
            plateFormationBias = pull(m.plateFormationBias, 0.4f),
            contractionMemory = pull(m.contractionMemory, 0.38f),
        ).clamped()
    }

    private fun parseState(s: String): InternalState? =
        runCatching { InternalState.valueOf(s.trim()) }.getOrNull()

    private fun load(): GrowthImprintModel {
        val raw = prefs.getString(KEY, null) ?: return GrowthImprintModel()
        return runCatching {
            val o = JSONObject(raw)
            GrowthImprintModel(
                stressLoad = o.optDouble("stressLoad", 0.35).toFloat(),
                calmReserve = o.optDouble("calmReserve", 0.4).toFloat(),
                recoveryStrength = o.optDouble("recoveryStrength", 0.35).toFloat(),
                disturbanceBias = o.optDouble("disturbanceBias", 0.25).toFloat(),
                stillnessAffinity = o.optDouble("stillnessAffinity", 0.3).toFloat(),
                chargeTrust = o.optDouble("chargeTrust", 0.35).toFloat(),
                asymmetryBias = o.optDouble("asymmetryBias", 0.25).toFloat(),
                branchingConfidence = o.optDouble("branchingConfidence", 0.4).toFloat(),
                plateFormationBias = o.optDouble("plateFormationBias", 0.35).toFloat(),
                contractionMemory = o.optDouble("contractionMemory", 0.3).toFloat(),
            ).clamped()
        }.getOrDefault(GrowthImprintModel())
    }

    private fun persist() {
        val m = model.clamped()
        val o = JSONObject().apply {
            put("stressLoad", m.stressLoad.toDouble())
            put("calmReserve", m.calmReserve.toDouble())
            put("recoveryStrength", m.recoveryStrength.toDouble())
            put("disturbanceBias", m.disturbanceBias.toDouble())
            put("stillnessAffinity", m.stillnessAffinity.toDouble())
            put("chargeTrust", m.chargeTrust.toDouble())
            put("asymmetryBias", m.asymmetryBias.toDouble())
            put("branchingConfidence", m.branchingConfidence.toDouble())
            put("plateFormationBias", m.plateFormationBias.toDouble())
            put("contractionMemory", m.contractionMemory.toDouble())
        }
        prefs.edit { putString(KEY, o.toString()) }
    }

    companion object {
        private const val PREFS = "velithorne_growth_imprint"
        private const val KEY = "imprint_v1"
    }
}
