package com.velithorne.innerway.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.velithorne.innerway.body.BatteryBloodSystem
import com.velithorne.innerway.body.CircadianRhythmSystem
import com.velithorne.innerway.body.MotionMuscleSystem
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
import com.velithorne.innerway.mind.GrowthStage
import com.velithorne.innerway.mind.InternalState
import com.velithorne.innerway.mind.InternalStateEngine
import com.velithorne.innerway.mind.SomaticHints
import com.velithorne.innerway.perception.EnvironmentalContext
import com.velithorne.innerway.perception.SensorFusion
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
    private val fusion = SensorFusion(
        battery = BatteryBloodSystem(application),
        thermal = ThermalBodySystem(application),
        nervous = NervousSystem(application),
        storage = StorageSkeletonSystem(application),
        motion = MotionMuscleSystem(),
        signal = SignalRespirationSystem(application),
        circadian = CircadianRhythmSystem(),
    )
    private val stateEngine = InternalStateEngine()

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

    private val _stage = MutableStateFlow(GrowthStage.SEED)
    val stage: StateFlow<GrowthStage> = _stage.asStateFlow()

    private val _lawContext = MutableStateFlow(SpeciesLaws.baselineContextForPhase1())
    val lawContext = _lawContext.asStateFlow()

    val memories: StateFlow<List<MemoryEntity>> = repository.observeRecent(24)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var stillnessSeconds = 0f
    private var motionAlertSeconds = 0f
    private var disturbanceScore = 0f
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
                val stillThreshold = 0.06f
                if (motion < stillThreshold) {
                    stillnessSeconds += dt
                } else {
                    stillnessSeconds = 0f
                }
                if (motion > 0.22f) {
                    motionAlertSeconds = max(motionAlertSeconds, 2.8f)
                    disturbanceScore = min(1f, disturbanceScore + 0.18f * min(1f, motion))
                }
                motionAlertSeconds = max(0f, motionAlertSeconds - dt)
                disturbanceScore = max(0f, disturbanceScore - 0.018f * dt)

                val hints = SomaticHints(
                    stillnessDurationSeconds = stillnessSeconds,
                    motionAlertSecondsRemaining = motionAlertSeconds,
                    disturbanceScore = disturbanceScore.coerceIn(0f, 1f),
                )
                _somaticHints.value = hints

                val resolved = stateEngine.resolve(env, hints)
                _internalState.value = resolved

                _bodyExpression.value = BodyExpressionMapper.map(resolved, env, hints)

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
    }

    fun refreshStage() {
        viewModelScope.launch {
            _stage.value = evolutionEngine.evaluateCurrentStage()
        }
    }

    companion object {
        private const val SAMPLE_MS = 1_000L
    }
}
