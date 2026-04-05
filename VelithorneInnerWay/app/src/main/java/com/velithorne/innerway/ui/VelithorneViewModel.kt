package com.velithorne.innerway.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.velithorne.innerway.VelithorneApplication
import com.velithorne.innerway.body.BatteryBloodSystem
import com.velithorne.innerway.body.CircadianRhythmSystem
import com.velithorne.innerway.body.NervousSystem
import com.velithorne.innerway.body.SignalRespirationSystem
import com.velithorne.innerway.body.StorageSkeletonSystem
import com.velithorne.innerway.body.ThermalBodySystem
import com.velithorne.innerway.genome.ActiveGenome
import com.velithorne.innerway.identity.SpeciesLaws
import com.velithorne.innerway.identity.VelithorneIdentity
import com.velithorne.innerway.memory.MemoryEntity
import com.velithorne.innerway.memory.MemoryDatabase
import com.velithorne.innerway.memory.MemoryRepository
import com.velithorne.innerway.mind.BodyExpressionMapper
import com.velithorne.innerway.mind.BodyExpressionModel
import com.velithorne.innerway.mind.EvolutionEngine
import com.velithorne.innerway.mind.GrowthImprintModel
import com.velithorne.innerway.mind.GrowthStage
import com.velithorne.innerway.mind.InternalState
import com.velithorne.innerway.mind.SomaticHints
import com.velithorne.innerway.perception.EnvironmentalContext
import com.velithorne.innerway.perception.SensorFusion
import com.velithorne.innerway.mind.TerritoryDebugStats
import com.velithorne.innerway.render.GrowthDebugStats
import com.velithorne.innerway.render.GrowthEngine
import com.velithorne.innerway.render.GrowthState
import com.velithorne.innerway.render.TerritoryMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class VelithorneViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MemoryRepository(MemoryDatabase.get(application).memoryDao())
    private val evolutionEngine = EvolutionEngine(repository)
    private val growthEngine = GrowthEngine()

    private val velithorneApp: VelithorneApplication
        get() = getApplication<VelithorneApplication>()

    private val fusion: SensorFusion
        get() = SensorFusion(
            battery = BatteryBloodSystem(velithorneApp),
            thermal = ThermalBodySystem(velithorneApp),
            nervous = NervousSystem(velithorneApp),
            storage = StorageSkeletonSystem(velithorneApp),
            motion = velithorneApp.motionMuscleSystem,
            signal = SignalRespirationSystem(velithorneApp),
            circadian = CircadianRhythmSystem(),
        )

    private val stateEngine
        get() = velithorneApp.internalStateEngine

    val identity: VelithorneIdentity = VelithorneIdentity.load(application)
    val activeGenome: ActiveGenome = ActiveGenome.load(application)

    private val _environment = MutableStateFlow(EnvironmentalContext())
    val environment: StateFlow<EnvironmentalContext> = _environment.asStateFlow()

    private val _internalState = MutableStateFlow(InternalState.CALM)
    val internalState: StateFlow<InternalState> = _internalState.asStateFlow()

    private val _somaticHints = MutableStateFlow(SomaticHints())
    val somaticHints: StateFlow<SomaticHints> = _somaticHints.asStateFlow()

    private val _bodyExpression = MutableStateFlow(BodyExpressionModel())
    val bodyExpression: StateFlow<BodyExpressionModel> = _bodyExpression.asStateFlow()

    private val _growthImprint = MutableStateFlow(velithorneApp.growthImprintEngine.current())
    val growthImprint: StateFlow<GrowthImprintModel> = _growthImprint.asStateFlow()

    private val _growthState = MutableStateFlow(GrowthState(emptyList(), emptyList(), emptyList()))
    val growthState: StateFlow<GrowthState> = _growthState.asStateFlow()

    private val _territoryMap = MutableStateFlow(velithorneApp.territoryEngine.snapshot())
    val territoryMap: StateFlow<TerritoryMap> = _territoryMap.asStateFlow()

    private val _territoryDebug = MutableStateFlow(velithorneApp.territoryEngine.debugStats())
    val territoryDebug: StateFlow<TerritoryDebugStats> = _territoryDebug.asStateFlow()

    private val _growthDebug = MutableStateFlow(
        GrowthDebugStats(
            nodeCount = 0,
            activeTipCount = 0,
            edgeCount = 0,
            plateCount = 0,
            growthRate = 0f,
            branchExtensionRate = 0f,
            maxNodesCap = 0,
            avgGrowthProgress = 0f,
            growingEdgesCount = 0,
            formingNodesCount = 0,
            matureVsGrowingRatio = 0f,
        ),
    )
    val growthDebug: StateFlow<GrowthDebugStats> = _growthDebug.asStateFlow()

    private val _growthCanvasPx = MutableStateFlow(400f to 260f)

    private val _stage = MutableStateFlow(GrowthStage.SEED)
    val stage: StateFlow<GrowthStage> = _stage.asStateFlow()

    private val _lawContext = MutableStateFlow(SpeciesLaws.baselineContextForPhase1())
    val lawContext = _lawContext.asStateFlow()

    val memories: StateFlow<List<MemoryEntity>> = repository.observeRecent(24)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var stillnessSeconds = 0f
    private var motionAlertSeconds = 0f
    private var disturbanceScore = 0f
    private var stableRecoverySeconds = 0f
    private var lastTickRealtime = SystemClock.elapsedRealtime()

    init {
        viewModelScope.launch {
            while (isActive) {
                val now = SystemClock.elapsedRealtime()
                val dt = ((now - lastTickRealtime) / 1000f).coerceIn(0f, 2.5f)
                lastTickRealtime = now

                val env = fusion.fuse()
                _environment.value = env

                val motion = env.motionEnergy.coerceIn(0f, 1f)

                if (motion > MOTION_STILL_THRESHOLD) {
                    stillnessSeconds = 0f
                } else {
                    stillnessSeconds += dt
                }

                if (motion > MOTION_SPIKE_FOR_ALERT) {
                    motionAlertSeconds = max(motionAlertSeconds, MOTION_ALERT_HOLD_SECONDS)
                    disturbanceScore = min(1f, disturbanceScore + 0.18f * min(1f, motion))
                }
                motionAlertSeconds = max(0f, motionAlertSeconds - dt)
                disturbanceScore = max(0f, disturbanceScore - 0.018f * dt)

                val energy = env.energyRatio.coerceIn(0f, 1f)
                val thermal = env.thermalRatio.coerceIn(0f, 1f)
                val nervous = env.nervousLoad.coerceIn(0f, 1f)
                val stableFrame =
                    energy > STABLE_ENERGY_MIN &&
                        thermal < STABLE_THERMAL_MAX &&
                        nervous < STABLE_NERVOUS_MAX &&
                        motion < STABLE_MOTION_MAX
                if (stableFrame) {
                    stableRecoverySeconds += dt
                } else {
                    stableRecoverySeconds = 0f
                }

                val hints = SomaticHints(
                    stillnessDurationSeconds = stillnessSeconds,
                    motionAlertSecondsRemaining = motionAlertSeconds,
                    disturbanceScore = disturbanceScore.coerceIn(0f, 1f),
                    stableRecoverySeconds = stableRecoverySeconds,
                )
                _somaticHints.value = hints

                val resolved = stateEngine.resolve(env, hints, dt)
                _internalState.value = resolved

                val expression = BodyExpressionMapper.map(resolved, env, hints)
                _bodyExpression.value = expression

                val memSnap = repository.recentSnapshot(48)
                _growthImprint.value = velithorneApp.growthImprintEngine.tick(
                    dt,
                    memSnap,
                    env,
                    resolved,
                    hints,
                )

                val count = repository.countMemories()
                _stage.value = evolutionEngine.evaluateCurrentStage()

                _lawContext.update {
                    SpeciesLaws.baselineContextForPhase1().copy(
                        memoryCount = count,
                        thermalDistress = env.thermalRatio > 0.82f,
                        lowPower = env.energyRatio < 0.18f && !env.charging,
                        deviceStable = env.thermalRatio < 0.92f,
                        currentStage = _stage.value,
                        targetStage = _stage.value,
                    )
                }
                delay(SAMPLE_MS)
            }
        }

        // Higher-frequency substrate growth (same persistent graph — incremental accretion)
        viewModelScope.launch {
            var lastG = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(GROWTH_TICK_MS)
                val now = SystemClock.elapsedRealtime()
                val dt = ((now - lastG) / 1000f).coerceIn(0f, 0.25f)
                lastG = now
                val st = _stage.value
                val stt = _internalState.value
                val ex = _bodyExpression.value
                val imprint = _growthImprint.value
                val (cw, ch) = _growthCanvasPx.value
                val env = _environment.value
                val hints = _somaticHints.value
                growthEngine.update(
                    st, stt, ex, imprint,
                    environment = env,
                    hints = hints,
                    territory = velithorneApp.territoryEngine,
                    dt = dt,
                    widthPx = cw,
                    heightPx = ch,
                )
                _growthState.value = growthEngine.snapshot()
                _growthDebug.value = growthEngine.debugStats(st, stt, ex, dt, imprint)
                _territoryMap.value = velithorneApp.territoryEngine.snapshot()
                _territoryDebug.value = velithorneApp.territoryEngine.debugStats()
            }
        }
    }

    fun recordSubstrateTouch(normalizedX: Float, normalizedY: Float) {
        velithorneApp.territoryEngine.recordTouchNormalized(normalizedX, normalizedY)
    }

    fun refreshStage() {
        viewModelScope.launch {
            _stage.value = evolutionEngine.evaluateCurrentStage()
        }
    }

    /** Canvas size in px — keeps growth step aspect aligned with substrate. */
    fun setGrowthCanvasSize(widthPx: Float, heightPx: Float) {
        if (widthPx > 10f && heightPx > 10f) {
            _growthCanvasPx.value = widthPx to heightPx
        }
    }

    companion object {
        private const val SAMPLE_MS = 1_000L
        private const val GROWTH_TICK_MS = 48L

        private const val MOTION_STILL_THRESHOLD = 0.06f
        private const val MOTION_SPIKE_FOR_ALERT = 0.22f
        private const val MOTION_ALERT_HOLD_SECONDS = 2.8f
        private const val STABLE_ENERGY_MIN = 0.35f
        private const val STABLE_THERMAL_MAX = 0.55f
        private const val STABLE_NERVOUS_MAX = 0.75f
        private const val STABLE_MOTION_MAX = 0.12f
    }
}
