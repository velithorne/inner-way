package com.velithorne.innerway

import android.app.Application
import androidx.work.Configuration
import com.velithorne.innerway.body.MotionMuscleSystem
import com.velithorne.innerway.boot.BootSequence
import com.velithorne.innerway.memory.MemoryDatabase
import com.velithorne.innerway.memory.MemoryRepository
import com.velithorne.innerway.mind.GrowthImprintEngine
import com.velithorne.innerway.mind.InternalStateEngine
import com.velithorne.innerway.mind.TerritoryEngine
import com.velithorne.innerway.services.SpeciesCycleWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application entry: wires persistent substrate (Room), asynchronous boot, periodic cycles,
 * and shared body/mind singletons (accelerometer, state engine).
 */
class VelithorneApplication : Application(), Configuration.Provider {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var motionMuscleSystem: MotionMuscleSystem
        private set

    lateinit var internalStateEngine: InternalStateEngine
        private set

    lateinit var growthImprintEngine: GrowthImprintEngine
        private set

    lateinit var territoryEngine: TerritoryEngine
        private set

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        motionMuscleSystem = MotionMuscleSystem(this)
        motionMuscleSystem.start()
        internalStateEngine = InternalStateEngine()
        growthImprintEngine = GrowthImprintEngine(this)
        territoryEngine = TerritoryEngine(this)

        val repository = MemoryRepository(MemoryDatabase.get(this).memoryDao())
        applicationScope.launch {
            BootSequence.run(this@VelithorneApplication, repository)
        }
        SpeciesCycleWorker.schedule(this)
    }

    override fun onTerminate() {
        motionMuscleSystem.stop()
        super.onTerminate()
    }
}
