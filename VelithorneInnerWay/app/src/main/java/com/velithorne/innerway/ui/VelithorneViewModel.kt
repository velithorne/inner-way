package com.velithorne.innerway.ui

import android.app.Application
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
import com.velithorne.innerway.mind.EvolutionEngine
import com.velithorne.innerway.mind.GrowthStage
import com.velithorne.innerway.mind.InternalState
import com.velithorne.innerway.perception.EnvironmentalContext
import com.velithorne.innerway.perception.SensorFusion
import com.velithorne.innerway.perception.StateInterpreter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
    private val interpreter = StateInterpreter()

    val identity: VelithorneIdentity = VelithorneIdentity.load(application)
    val activeGenome: ActiveGenome = ActiveGenome.load(application)

    private val _environment = MutableStateFlow(EnvironmentalContext())
    val environment: StateFlow<EnvironmentalContext> = _environment.asStateFlow()

    private val _internalState = MutableStateFlow(InternalState.CALM)
    val internalState: StateFlow<InternalState> = _internalState.asStateFlow()

    private val _stage = MutableStateFlow(GrowthStage.SEED)
    val stage: StateFlow<GrowthStage> = _stage.asStateFlow()

    private val _lawContext = MutableStateFlow(SpeciesLaws.baselineContextForPhase1())
    val lawContext = _lawContext.asStateFlow()

    val memories: StateFlow<List<MemoryEntity>> = repository.observeRecent(24)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            while (isActive) {
                val env = fusion.fuse()
                _environment.value = env
                _internalState.value = interpreter.interpret(env)
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
        private const val SAMPLE_MS = 5_000L
    }
}
