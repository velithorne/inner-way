package com.velithorne.vessel.morphogenesis

import com.velithorne.vessel.physiology.PhysiologySnapshot
import com.velithorne.vessel.physiology.SpeciesState
import com.velithorne.vessel.telemetry.NetworkTransport
import com.velithorne.vessel.telemetry.TelemetrySnapshot
import com.velithorne.vessel.util.Smoothing
import kotlin.math.abs
import kotlin.math.floor

/**
 * Telemetry + physiology → growth pressures → graph → contour (incremental).
 */
class MorphogenesisEngine(
    private val tuning: GrowthTuning = GrowthTuning(),
    lineageId: Long = 0x4E564C_01L,
) {
    private val seed = SpeciesSeed.fromLineageId(lineageId)
    private var genome: SpeciesGenome = SpeciesGenome.initial(seed)
    private var accumulator = PressureAccumulator()
    private var field = GrowthPressureField(0f, 0f, 0f, 0f, 0f, 0f)
    private var graph: StructuralGraph? = null
    private var contour: ContourParams? = null
    private var tissue: TissueEnvelopeState? = null
    private var pathwaySpec: PathwaySpec? = null
    private var growthPhase = 0f
    private val eventLog = ArrayDeque<GrowthEvent>(16)

    fun update(snapshot: PhysiologySnapshot): MorphogenesisSnapshot {
        val telem = snapshot.telemetry
        val species = snapshot.species
        val raw = derivePressures(telem, species)
        val alpha = tuning.pressureSmoothingAlpha
        accumulator = accumulator.step(raw, alpha)

        field = diffuseField(accumulator, genome)

        genome = driftGenome(genome, accumulator, alpha * 0.4f)

        graph = OrganPlacementEngine.buildGraph(seed, genome, field, species, graph, tuning)
        contour = ContourGrowthEngine.step(contour, genome, field, accumulator, tuning)
        tissue = TissueLayerGenerator.envelope(genome, field, accumulator, tissue, tuning)
        pathwaySpec = PathwayGrowthEngine.spec(graph!!, genome, accumulator, tuning, pathwaySpec)

        growthPhase += 0.018f + accumulator.signal * 0.01f + accumulator.thermal * 0.008f
        if (growthPhase > 1000f) growthPhase -= 1000f

        val visible = computeVisibleGrowth(accumulator, field)
        maybeAppendEvents(snapshot.timestampMillis, accumulator)

        val explainer = GrowthExplainer.explain(accumulator, genome, field, tuning)
        val statusLabel = GrowthExplainer.statusLabel(accumulator, visible)
        val statusLine = GrowthExplainer.statusLine(accumulator)

        return MorphogenesisSnapshot(
            timestampMillis = snapshot.timestampMillis,
            seed = seed,
            genome = genome,
            pressures = raw,
            accumulated = accumulator,
            field = field,
            graph = graph!!,
            contour = contour!!,
            tissue = tissue!!,
            pathways = pathwaySpec!!,
            visibleGrowthActivity = visible,
            growthStatusLabel = statusLabel,
            growthStatusLine = statusLine,
            explainerLines = explainer,
        )
    }

    private fun derivePressures(telem: TelemetrySnapshot, s: SpeciesState): GrowthPressure {
        val thermal = (s.fever * 0.55f + (telem.batteryTempC?.let { t ->
            ((t - 30f) / 22f).coerceIn(0f, 1f)

        } ?: 0f) * 0.35f).coerceIn(0f, 1f)
        val hunger = s.hunger
        val reserve = (1f - hunger) * 0.55f + (if (telem.isCharging == true) 0.35f else 0f)
        val reservePressure = (1f - reserve).coerceIn(0f, 1f)
        val signal = (s.signalArousal * 0.45f + (if (telem.networkMetered == true) 0.25f else 0f) +
            (if (telem.networkMetered == true && telem.networkType == NetworkTransport.CELLULAR) 0.15f else 0f)).coerceIn(0f, 1f)
        val archive = (s.structuralLoad * 0.65f + (telem.storageUsedPct ?: 0f) * 0.0035f).coerceIn(0f, 1f)
        val motion = (telem.motionIntensity ?: 0f).coerceIn(0f, 1f) * 0.85f + s.mobility * 0.15f
        val neural = (s.neuralActivity * 0.55f + (if (telem.screenInteractive == true) 0.2f else 0f) +
            (if (telem.lowMemoryFlag == true) 0.35f else 0f)).coerceIn(0f, 1f)
        val recovery = s.recovery
        val sleep = s.sleepPressure
        val attachment = environmentalAttachment(telem)
        return GrowthPressure(
            thermalPressure = thermal,
            reservePressure = reservePressure,
            hungerPressure = hunger,
            signalPressure = signal,
            archivePressure = archive,
            motionPressure = motion,
            neuralPressure = neural,
            recoveryPressure = recovery,
            sleepPressure = sleep,
            environmentalAttachmentPressure = attachment,
        )
    }

    private fun environmentalAttachment(telem: TelemetrySnapshot): Float {
        var a = 0f
        if (telem.isCharging == true) a += 0.35f
        if (telem.networkConnected == true && telem.networkType != NetworkTransport.NONE) a += 0.25f
        if (telem.networkType == NetworkTransport.BLUETOOTH) a += 0.15f
        if (telem.networkType == NetworkTransport.ETHERNET) a += 0.1f
        return a.coerceIn(0f, 1f)
    }

    private fun diffuseField(acc: PressureAccumulator, g: SpeciesGenome): GrowthPressureField {
        val cortical = (acc.neural * 0.45f + acc.sleep * 0.15f + g.cranialExpansionBias * 0.25f).coerceIn(0f, 1f)
        val central = (acc.reserve * 0.35f + acc.recovery * 0.35f + acc.hunger * 0.2f).coerceIn(0f, 1f)
        val lateral = (acc.signal * 0.55f + acc.motion * 0.25f + g.antennaBranchBias * 0.2f).coerceIn(0f, 1f)
        val lower = (acc.archive * 0.55f + acc.hunger * 0.25f + g.lowerReservoirBias * 0.2f).coerceIn(0f, 1f)
        val shell = (acc.thermal * 0.45f + g.shellThickness * 0.35f + acc.attachment * 0.15f).coerceIn(0f, 1f)
        val tendon = (acc.motion * 0.5f + (1f - acc.sleep) * 0.15f + g.tendonDensityBias * 0.25f).coerceIn(0f, 1f)
        val fa = tuning.fieldDiffuseAlpha
        val prev = field
        return GrowthPressureField(
            cortical = Smoothing.lerp(prev.cortical, cortical, fa),
            centralChamber = Smoothing.lerp(prev.centralChamber, central, fa),
            lateralSignal = Smoothing.lerp(prev.lateralSignal, lateral, fa),
            lowerArchive = Smoothing.lerp(prev.lowerArchive, lower, fa),
            perimeterShell = Smoothing.lerp(prev.perimeterShell, shell, fa),
            supportTendon = Smoothing.lerp(prev.supportTendon, tendon, fa),
        )
    }

    private fun driftGenome(g: SpeciesGenome, acc: PressureAccumulator, a: Float): SpeciesGenome = SpeciesGenome(
        shellThickness = Smoothing.lerp(g.shellThickness, (g.shellThickness + acc.thermal * tuning.shellThickenThermal * 0.02f).coerceIn(0.2f, 1f), a),
        organSpacingBias = Smoothing.lerp(g.organSpacingBias, g.organSpacingBias + acc.signal * 0.01f - acc.archive * 0.008f, a),
        cranialExpansionBias = Smoothing.lerp(g.cranialExpansionBias, g.cranialExpansionBias + acc.neural * tuning.cranialExpandNeural * 0.015f, a),
        lowerReservoirBias = Smoothing.lerp(g.lowerReservoirBias, g.lowerReservoirBias + acc.archive * tuning.archiveExpandLoad * 0.018f, a),
        antennaBranchBias = Smoothing.lerp(g.antennaBranchBias, g.antennaBranchBias + acc.signal * tuning.lateralSpreadSignal * 0.02f, a),
        coolingVeilBias = Smoothing.lerp(g.coolingVeilBias, g.coolingVeilBias + acc.thermal * 0.022f, a),
        archiveLamellaBias = Smoothing.lerp(g.archiveLamellaBias, g.archiveLamellaBias + acc.archive * 0.025f, a),
        tendonDensityBias = Smoothing.lerp(g.tendonDensityBias, g.tendonDensityBias + acc.motion * 0.02f, a),
        conduitDensityBias = Smoothing.lerp(g.conduitDensityBias, g.conduitDensityBias + acc.signal * 0.018f, a),
        asymmetryBias = Smoothing.lerp(g.asymmetryBias, (g.asymmetryBias + (acc.signal - 0.5f) * 0.01f).coerceIn(0f, 0.45f), a),
        resilienceBias = Smoothing.lerp(g.resilienceBias, g.resilienceBias + acc.recovery * 0.015f - acc.hunger * 0.01f, a),
    )

    private fun computeVisibleGrowth(acc: PressureAccumulator, f: GrowthPressureField): Float {
        val frac = growthPhase - floor(growthPhase.toDouble()).toFloat()
        val v = (acc.signal * 0.25f + acc.thermal * 0.25f + acc.neural * 0.2f + f.lateralSignal * 0.15f + frac * 0.15f)
        return (v * tuning.visibleGrowthPulseScale).coerceIn(0f, 1f)
    }

    private fun maybeAppendEvents(ts: Long, acc: PressureAccumulator) {
        if (acc.thermal > 0.65f && eventLog.lastOrNull()?.kind != GrowthRule.THERMAL_VEIL) {
            eventLog.addLast(GrowthEvent(ts, GrowthRule.THERMAL_VEIL, "thermal high", "t=${acc.thermal}"))
        }
        if (acc.signal > 0.62f && eventLog.lastOrNull()?.kind != GrowthRule.SIGNAL_FROND) {
            eventLog.addLast(GrowthEvent(ts, GrowthRule.SIGNAL_FROND, "signal branch", "s=${acc.signal}"))
        }
        while (eventLog.size > 12) eventLog.removeFirst()
    }
}
