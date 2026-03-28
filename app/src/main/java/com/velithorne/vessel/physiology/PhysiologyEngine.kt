package com.velithorne.vessel.physiology

import com.velithorne.vessel.telemetry.NetworkTransport
import com.velithorne.vessel.telemetry.TelemetrySnapshot
import com.velithorne.vessel.util.Smoothing
import kotlin.math.abs
import kotlin.math.min

/**
 * Deterministic telemetry → synthetic biology. Stateless formulas; internal EMA for stability.
 *
 * Phase 3: [com.velithorne.vessel.domain.phase2.VesselRenderer] consumes [PhysiologySnapshot].
 * Phase 4+: evolution engine may log long-horizon integrals of these streams (not implemented here).
 */
class PhysiologyEngine(
    private val tuning: PhysiologyTuning = PhysiologyTuning(),
) {

    private var prevVitality: Float? = null
    private var prevStress: Float? = null
    private var prevHunger: Float? = null
    private var prevFever: Float? = null
    private var prevRecovery: Float? = null
    private var prevRespiration: Float? = null
    private var prevNeural: Float? = null
    private var prevMobility: Float? = null
    private var prevSleep: Float? = null
    private var prevSignal: Float? = null
    private var prevStructural: Float? = null

    private var prevPitch: Float? = null
    private var prevRoll: Float? = null

    /** Last derived input bundle (for tests / future debug overlays). */
    var lastInputs: PhysiologyInputs? = null
        private set

    fun update(telemetry: TelemetrySnapshot): PhysiologySnapshot {
        val batteryNorm = ((telemetry.batteryPct ?: 50f) / 100f).coerceIn(0f, 1f)
        val meteredStrain = meteredStrain(telemetry)

        val hungerRaw = computeHungerRaw(telemetry, batteryNorm)
        val feverRaw = computeFeverRaw(telemetry)
        val thermalStrain = computeThermalStrain(telemetry, feverRaw)

        val memoryStrain = computeMemoryStrain(telemetry)
        val storageStrain = computeStorageStrain(telemetry)

        val motionNorm = (telemetry.motionIntensity ?: 0f) / tuning.motionIntensityFullScale
        val orientationDelta = orientationDeltaNorm(telemetry)
        val energyDeficit = computeEnergyDeficit(telemetry, batteryNorm)

        val stressRaw = StressModel.aggregate(
            thermalStrain = thermalStrain,
            memoryStrain = memoryStrain,
            storageStrain = storageStrain,
            hunger = hungerRaw,
            energyDeficit = energyDeficit,
            motionNorm = motionNorm.coerceIn(0f, 1f),
            meteredStrain = meteredStrain,
        )

        val vitalityRaw = computeVitalityRaw(
            telemetry = telemetry,
            batteryNorm = batteryNorm,
            thermalStrain = thermalStrain,
            memoryStrain = memoryStrain,
            storageStrain = storageStrain,
            hungerRaw = hungerRaw,
        )

        val neuralRaw = computeNeuralRaw(telemetry, memoryStrain, motionNorm)
        val mobilityRaw = (motionNorm * 0.62f + orientationDelta * 0.55f).coerceIn(0f, 1f)

        val respirationRaw = networkRespirationRaw(telemetry)
        val circadianNight = CircadianModel.nightBias01(telemetry.timestampMillis, tuning)
        val uptimeHours = telemetry.uptimeMillis / 3_600_000f
        val uptimeFatigue = (uptimeHours / tuning.uptimeSaturationHours).coerceIn(0f, 1f)

        val screenOn = telemetry.screenInteractive == true
        val calmFactor = ((1f - motionNorm.coerceIn(0f, 1f)) * (1f - neuralRaw * 0.35f)).coerceIn(0f, 1f)
        val sleepRaw = (
            uptimeFatigue * 0.42f +
                calmFactor * 0.28f +
                circadianNight * 0.38f -
                (if (screenOn) 0.18f else 0f)
            ).coerceIn(0f, 1f)

        val signalRaw = signalArousalRaw(telemetry, motionNorm)
        val structuralRaw = (
            storageStrain * 0.52f +
                memoryStrain * 0.38f +
                (if (telemetry.powerSaveEnabled == true) 0.12f else 0f)
            ).coerceIn(0f, 1f)

        val recoveryTarget = RecoveryModel.target(
            isCharging = telemetry.isCharging,
            screenInteractive = telemetry.screenInteractive,
            motionNorm = motionNorm.coerceIn(0f, 1f),
            feverRaw = feverRaw,
            thermalStrain = thermalStrain,
            hunger = hungerRaw,
            stress = stressRaw,
        )

        prevVitality = smooth(prevVitality, vitalityRaw, tuning.smoothVitality)
        prevStress = smooth(prevStress, stressRaw, tuning.smoothStress)
        prevHunger = smooth(prevHunger, hungerRaw, tuning.smoothHunger)
        prevFever = smooth(prevFever, feverRaw, tuning.smoothFever)
        prevRecovery = smooth(prevRecovery, recoveryTarget, tuning.smoothRecovery)
        prevRespiration = smooth(prevRespiration, respirationRaw, tuning.smoothRespiration)
        prevNeural = smooth(prevNeural, neuralRaw, tuning.smoothNeural)
        prevMobility = smooth(prevMobility, mobilityRaw, tuning.smoothMobility)
        prevSleep = smooth(prevSleep, sleepRaw, tuning.smoothSleep)
        prevSignal = smooth(prevSignal, signalRaw, tuning.smoothSignal)
        prevStructural = smooth(prevStructural, structuralRaw, tuning.smoothStructural)

        val vitality = prevVitality ?: vitalityRaw
        val stress = prevStress ?: stressRaw
        val hunger = prevHunger ?: hungerRaw
        val fever = prevFever ?: feverRaw
        val recovery = prevRecovery ?: recoveryTarget
        val respiration = prevRespiration ?: respirationRaw
        val neural = prevNeural ?: neuralRaw
        val mobility = prevMobility ?: mobilityRaw
        val sleepPressure = prevSleep ?: sleepRaw
        val signalArousal = prevSignal ?: signalRaw
        val structural = prevStructural ?: structuralRaw

        val healthLabel = classifyHealth(vitality, stress, fever, hunger, structural)
        val summary = buildSummary(
            vitality = vitality,
            stress = stress,
            hunger = hunger,
            fever = fever,
            recovery = recovery,
            charging = telemetry.isCharging,
            structural = structural,
            thermalBlind = telemetry.batteryTempC == null,
        )

        val species = SpeciesState(
            vitality = vitality,
            stress = stress,
            hunger = hunger,
            fever = fever,
            recovery = recovery,
            respiration = respiration,
            neuralActivity = neural,
            mobility = mobility,
            sleepPressure = sleepPressure,
            signalArousal = signalArousal,
            structuralLoad = structural,
            healthLabel = healthLabel,
            stateSummary = summary,
        )

        val organs = mapOrgans(
            telemetry = telemetry,
            species = species,
            batteryNorm = batteryNorm,
            memoryStrain = memoryStrain,
            storageStrain = storageStrain,
            feverRaw = feverRaw,
        )

        lastInputs = PhysiologyInputs(
            batteryNorm = batteryNorm,
            hungerRaw = hungerRaw,
            feverRaw = feverRaw,
            thermalStrain = thermalStrain,
            memoryStrain = memoryStrain,
            storageStrain = storageStrain,
            networkRespirationRaw = respirationRaw,
            motionNorm = motionNorm.coerceIn(0f, 1f),
            orientationDeltaNorm = orientationDelta,
            uptimeHours = uptimeHours,
            circadianPhase01 = CircadianModel.phase01(telemetry.timestampMillis),
        )

        return PhysiologySnapshot(
            timestampMillis = telemetry.timestampMillis,
            telemetry = telemetry,
            species = species,
            organs = organs,
        )
    }

    private fun smooth(prev: Float?, next: Float, alpha: Float): Float {
        val clamped = next.coerceIn(0f, 1f)
        return Smoothing.exponentialMovingAverage(prev, clamped, alpha) ?: clamped
    }

    private fun computeHungerRaw(telemetry: TelemetrySnapshot, batteryNorm: Float): Float {
        val bn = batteryNorm.coerceIn(0f, 1f)
        var h = 1f - bn
        when {
            telemetry.isCharging == true -> h *= 0.58f
            telemetry.isCharging == false -> { /* full hunger signal */ }
            else -> h *= 0.82f
        }
        val famineBoost = (1f - inverseLerp(
            tuning.hungerBatteryFamineBelow / 100f,
            0.65f.coerceAtLeast(tuning.hungerBatteryFamineBelow / 100f + 0.08f),
            bn,
        )).coerceIn(0f, 1f)
        val satiety = inverseLerp(tuning.hungerBatterySatietyAbove / 100f, 0.95f, bn)
        h = (h * 0.55f + famineBoost * 0.28f + (1f - satiety) * 0.22f).coerceIn(0f, 1f)
        if (telemetry.batteryPct == null) {
            h = h * 0.75f + 0.25f
        }
        return h.coerceIn(0f, 1f)
    }

    private fun computeFeverRaw(telemetry: TelemetrySnapshot): Float {
        val t = telemetry.batteryTempC ?: return 0f
        return when {
            t <= tuning.thermalCoolMaxC -> {
                val x = inverseLerp(15f, tuning.thermalCoolMaxC, t.coerceIn(15f, tuning.thermalCoolMaxC))
                x * 0.22f
            }
            t <= tuning.thermalWarmC -> {
                val x = inverseLerp(tuning.thermalCoolMaxC, tuning.thermalWarmC, t)
                0.22f + x * 0.38f
            }
            else -> {
                val x = inverseLerp(tuning.thermalWarmC, tuning.thermalHotC + 6f, t.coerceAtMost(tuning.thermalHotC + 10f))
                0.6f + x * 0.45f
            }
        }.coerceIn(0f, 1f)
    }

    private fun computeThermalStrain(telemetry: TelemetrySnapshot, fever: Float): Float {
        if (telemetry.batteryTempC != null) {
            return fever
        }
        var s = 0f
        if (telemetry.powerSaveEnabled == true) s += 0.18f
        return s.coerceIn(0f, 1f)
    }

    private fun computeMemoryStrain(telemetry: TelemetrySnapshot): Float {
        var s = 0.14f
        if (telemetry.lowMemoryFlag == true) s = maxOf(s, 0.88f)
        if (telemetry.lowRamDevice == true) s += 0.22f
        if (telemetry.lowMemoryFlag == false && telemetry.lowRamDevice == false) s = min(s, 0.18f)
        if (telemetry.lowMemoryFlag == null && telemetry.lowRamDevice == null) s = 0.22f
        return s.coerceIn(0f, 1f)
    }

    private fun computeStorageStrain(telemetry: TelemetrySnapshot): Float {
        val pct = telemetry.storageUsedPct ?: return 0.18f
        return inverseLerp(tuning.storageMidPct, tuning.storageHighPct, pct).coerceIn(0f, 1f)
    }

    private fun computeEnergyDeficit(telemetry: TelemetrySnapshot, batteryNorm: Float): Float {
        val deficit = 1f - batteryNorm.coerceIn(0f, 1f)
        return if (telemetry.isCharging == true) deficit * 0.35f else deficit
    }

    private fun computeVitalityRaw(
        telemetry: TelemetrySnapshot,
        batteryNorm: Float,
        thermalStrain: Float,
        memoryStrain: Float,
        storageStrain: Float,
        hungerRaw: Float,
    ): Float {
        val bn = batteryNorm.coerceIn(0f, 1f)
        var v = 0.52f + bn * 0.28f
        v += when (telemetry.networkConnected) {
            true -> 0.07f
            false -> -0.08f
            null -> 0f
        }
        v -= thermalStrain * 0.22f
        v -= memoryStrain * 0.24f
        v -= storageStrain * 0.15f
        v -= hungerRaw * 0.11f
        if (telemetry.lowMemoryFlag == true) v -= 0.12f
        if (telemetry.powerSaveEnabled == true) v -= 0.05f
        return v.coerceIn(0f, 1f)
    }

    private fun computeNeuralRaw(
        telemetry: TelemetrySnapshot,
        memoryStrain: Float,
        motionNorm: Float,
    ): Float {
        val screen = when (telemetry.screenInteractive) {
            true -> 0.38f
            false -> 0.14f
            null -> 0.22f
        }
        val ps = if (telemetry.powerSaveEnabled == true) 0.14f else 0f
        return (0.14f + memoryStrain * 0.34f + screen + motionNorm.coerceIn(0f, 1f) * 0.28f + ps).coerceIn(0f, 1f)
    }

    private fun networkRespirationRaw(t: TelemetrySnapshot): Float {
        val connected = t.networkConnected
        if (connected == false) return 0.12f
        var base = when (t.networkType) {
            NetworkTransport.WIFI, NetworkTransport.ETHERNET, NetworkTransport.VPN -> 0.78f
            NetworkTransport.CELLULAR -> 0.52f
            NetworkTransport.BLUETOOTH -> 0.34f
            NetworkTransport.OTHER -> 0.45f
            NetworkTransport.NONE -> if (connected == true) 0.35f else 0.1f
        }
        if (t.networkMetered == true) base = (base * 0.85f + 0.15f).coerceIn(0f, 1f)
        if (connected == null) base *= 0.85f
        return base.coerceIn(0f, 1f)
    }

    private fun signalArousalRaw(t: TelemetrySnapshot, motionNorm: Float): Float {
        var s = 0.12f
        if (t.networkConnected == true) s += 0.32f
        if (t.networkConnected == false) s += 0.04f
        s += when (t.networkType) {
            NetworkTransport.WIFI, NetworkTransport.ETHERNET -> 0.18f
            NetworkTransport.CELLULAR -> 0.12f
            NetworkTransport.VPN -> 0.15f
            else -> 0.06f
        }
        s += motionNorm.coerceIn(0f, 1f) * 0.22f
        if (t.screenInteractive == true) s += 0.14f
        return s.coerceIn(0f, 1f)
    }

    private fun meteredStrain(t: TelemetrySnapshot): Float {
        return if (t.networkType == NetworkTransport.CELLULAR && t.networkMetered == true) 0.38f else 0f
    }

    private fun orientationDeltaNorm(t: TelemetrySnapshot): Float {
        val p = t.orientationPitchDeg
        val r = t.orientationRollDeg
        if (p == null || r == null) {
            prevPitch = p
            prevRoll = r
            return 0f
        }
        val pp = prevPitch
        val pr = prevRoll
        prevPitch = p
        prevRoll = r
        if (pp == null || pr == null) return 0f
        val delta = abs(p - pp) + abs(r - pr)
        return (delta / tuning.orientationDeltaFullScaleDeg).coerceIn(0f, 1f)
    }

    private fun mapOrgans(
        telemetry: TelemetrySnapshot,
        species: SpeciesState,
        batteryNorm: Float,
        memoryStrain: Float,
        storageStrain: Float,
        feverRaw: Float,
    ): List<OrganState> {
        val bn = batteryNorm.coerceIn(0f, 1f)
        val heartNote = when {
            telemetry.isCharging == true && bn > 0.35f -> "Regeneration active"
            species.hunger > 0.72f && bn < 0.28f -> "Entering deficit"
            bn > 0.55f -> "Reserve stable"
            else -> "Metabolic pacing nominal"
        }
        val metabolic = OrganState(
            organType = OrganType.METABOLIC_HEART,
            health = mix(species.vitality, 1f - species.hunger, 0.5f),
            load = (species.hunger * 0.55f + species.stress * 0.35f).coerceIn(0f, 1f),
            activity = if (telemetry.isCharging == true) 0.85f else 0.4f + bn * 0.35f,
            inflammation = (feverRaw * 0.35f + species.stress * 0.25f).coerceIn(0f, 1f),
            reserve = bn,
            note = heartNote,
        )

        val cortex = OrganState(
            organType = OrganType.CORTEX_CLUSTER,
            health = (1f - species.stress * 0.55f - memoryStrain * 0.25f).coerceIn(0f, 1f),
            load = (species.neuralActivity * 0.5f + species.stress * 0.35f).coerceIn(0f, 1f),
            activity = species.neuralActivity,
            inflammation = (species.stress * 0.3f + memoryStrain * 0.35f).coerceIn(0f, 1f),
            reserve = (1f - species.neuralActivity * 0.5f).coerceIn(0f, 1f),
            note = when {
                telemetry.screenInteractive == true && species.neuralActivity > 0.55f -> "High sensory throughput"
                telemetry.powerSaveEnabled == true -> "Cortex throttled — efficiency mode"
                memoryStrain > 0.55f -> "Attention bandwidth constrained"
                else -> "Cognitive mesh synchronized"
            },
        )

        val gel = OrganState(
            organType = OrganType.NEURAL_GEL,
            health = (1f - memoryStrain).coerceIn(0f, 1f),
            load = memoryStrain,
            activity = species.neuralActivity,
            inflammation = (memoryStrain * 0.7f + feverRaw * 0.15f).coerceIn(0f, 1f),
            reserve = (1f - memoryStrain * 0.85f).coerceIn(0f, 1f),
            note = when {
                telemetry.lowMemoryFlag == true -> "Synaptic buffer critical"
                telemetry.lowRamDevice == true -> "Low-RAM substrate — shallow buffers"
                else -> "Plasticity reservoir within norm"
            },
        )

        val archiveNote = when {
            storageStrain > 0.72f -> "Archive congested"
            storageStrain > 0.4f -> "Archive dense"
            else -> "Archive stable"
        }
        val archive = OrganState(
            organType = OrganType.ARCHIVE_VAULT,
            health = (1f - storageStrain * 0.65f - species.structuralLoad * 0.2f).coerceIn(0f, 1f),
            load = (storageStrain * 0.7f + species.structuralLoad * 0.35f).coerceIn(0f, 1f),
            activity = (species.structuralLoad * 0.45f + storageStrain * 0.35f).coerceIn(0f, 1f),
            inflammation = (species.structuralLoad * 0.25f + storageStrain * 0.2f).coerceIn(0f, 1f),
            reserve = (1f - (telemetry.storageUsedPct ?: 50f) / 100f).coerceIn(0f, 1f),
            note = archiveNote,
        )

        val lungs = OrganState(
            organType = OrganType.SIGNAL_LUNGS,
            health = (species.respiration * 0.6f + (1f - species.stress * 0.35f)).coerceIn(0f, 1f),
            load = (1f - species.respiration) * 0.55f + species.signalArousal * 0.25f,
            activity = species.respiration,
            inflammation = ((1f - species.respiration) * species.signalArousal).coerceIn(0f, 1f),
            reserve = species.signalArousal,
            note = when {
                telemetry.networkConnected != true -> "Gas exchange shallow — signal drought"
                telemetry.networkType == NetworkTransport.CELLULAR && telemetry.networkMetered == true ->
                    "Mobile channel metered — labored breath"

                else -> "Atmospheric exchange nominal"
            },
        )

        val vest = OrganState(
            organType = OrganType.VESTIBULAR_MUSCULATURE,
            health = (1f - species.mobility * 0.25f - species.stress * 0.2f).coerceIn(0f, 1f),
            load = species.mobility,
            activity = species.mobility,
            inflammation = (species.stress * 0.2f + species.mobility * feverRaw * 0.15f).coerceIn(0f, 1f),
            reserve = (1f - species.mobility).coerceIn(0f, 1f),
            note = if (species.mobility > 0.55f) "Locomotor storm — posture hunting" else "Stance lattice quiet",
        )

        val thermal = OrganState(
            organType = OrganType.THERMAL_MEMBRANE,
            health = (1f - species.fever - species.stress * 0.25f).coerceIn(0f, 1f),
            load = (feverRaw * 0.65f + species.stress * 0.35f).coerceIn(0f, 1f),
            activity = species.fever,
            inflammation = (species.fever * 0.85f + feverRaw * 0.15f).coerceIn(0f, 1f),
            reserve = (1f - species.fever).coerceIn(0f, 1f),
            note = when {
                telemetry.batteryTempC == null -> "Thermal sensors occluded"
                species.fever > 0.55f -> "Active cooling advised"
                species.fever > 0.3f -> "Membrane warm — monitoring"
                else -> "Membrane within thermal band"
            },
        )

        return listOf(metabolic, cortex, gel, archive, lungs, vest, thermal)
    }

    private fun mix(a: Float, b: Float, t: Float): Float = (a * (1f - t) + b * t).coerceIn(0f, 1f)

    private fun inverseLerp(low: Float, high: Float, value: Float): Float {
        if (high <= low) return 0f
        return ((value - low) / (high - low)).coerceIn(0f, 1f)
    }

    private fun classifyHealth(
        vitality: Float,
        stress: Float,
        fever: Float,
        hunger: Float,
        structural: Float,
    ): String {
        return when {
            vitality < 0.28f || stress > 0.82f -> "Critical"
            (hunger > 0.78f && vitality < 0.45f) || structural > 0.85f -> "Strained"
            fever > 0.68f && stress > 0.55f -> "Febrile strain"
            vitality > 0.65f && stress < 0.38f -> "Stable"
            stress > 0.55f -> "Strained"
            else -> "Adaptive"
        }
    }

    private fun buildSummary(
        vitality: Float,
        stress: Float,
        hunger: Float,
        fever: Float,
        recovery: Float,
        charging: Boolean?,
        structural: Float,
        thermalBlind: Boolean,
    ): String {
        val parts = mutableListOf<String>()
        parts += when {
            vitality > 0.62f && stress < 0.42f -> "Vessel stable."
            vitality < 0.38f || stress > 0.65f -> "Vessel strained."
            else -> "Vessel compensating."
        }
        when {
            hunger > 0.65f -> parts += "Energy deficit rising."
            hunger < 0.35f -> parts += "Reserves adequate."
        }
        when {
            fever > 0.55f -> parts += "Elevated thermal setpoint."
            fever > 0.3f -> parts += "Mild fever."
        }
        when {
            structural > 0.65f -> parts += "Archive burden elevated."
            structural < 0.35f -> parts += "Structural load light."
        }
        when {
            charging == true && recovery > 0.55f -> parts += "Recovery under charge."
            charging == false && recovery > 0.55f -> parts += "Passive recovery."
        }
        if (thermalBlind && fever < 0.2f) {
            parts += "Thermal readout partial."
        }
        return parts.joinToString(" ")
    }
}
