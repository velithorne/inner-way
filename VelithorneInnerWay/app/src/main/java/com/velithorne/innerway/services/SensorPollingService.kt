package com.velithorne.innerway.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.velithorne.innerway.body.BatteryBloodSystem
import com.velithorne.innerway.body.CircadianRhythmSystem
import com.velithorne.innerway.body.MotionMuscleSystem
import com.velithorne.innerway.body.NervousSystem
import com.velithorne.innerway.body.SignalRespirationSystem
import com.velithorne.innerway.body.StorageSkeletonSystem
import com.velithorne.innerway.body.ThermalBodySystem
import com.velithorne.innerway.memory.EventLogger
import com.velithorne.innerway.memory.MemoryDatabase
import com.velithorne.innerway.memory.MemoryKind
import com.velithorne.innerway.memory.MemoryRepository
import com.velithorne.innerway.VelithorneApplication
import com.velithorne.innerway.mind.InternalState
import com.velithorne.innerway.perception.SensorFusion
import com.velithorne.innerway.perception.StateInterpreter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Samples body truth on a conservative cadence while running.
 * Persists on first observation and whenever interpreted state changes.
 */
class SensorPollingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (pollJob?.isActive == true) return START_STICKY
        val repository = MemoryRepository(MemoryDatabase.get(this).memoryDao())
        val logger = EventLogger(repository)
        val app = applicationContext as VelithorneApplication
        val fusion = SensorFusion(
            battery = BatteryBloodSystem(this),
            thermal = ThermalBodySystem(this),
            nervous = NervousSystem(this),
            storage = StorageSkeletonSystem(this),
            motion = app.motionMuscleSystem,
            signal = SignalRespirationSystem(this),
            circadian = CircadianRhythmSystem(),
        )
        val interpreter = StateInterpreter()
        var lastState: InternalState? = null

        pollJob = scope.launch {
            while (isActive) {
                val env = fusion.fuse()
                val state = interpreter.interpret(env)
                val shouldLog = lastState == null || state != lastState
                if (shouldLog) {
                    logger.log(
                        bodySummary = "e=${"%.2f".format(env.energyRatio)} th=${"%.2f".format(env.thermalRatio)} n=${"%.2f".format(env.nervousLoad)}",
                        interpretedState = state.name,
                        behavior = if (lastState == null) "substrate_first_sample" else "state_transition",
                        important = if (lastState == null) "session_start" else "shift:${lastState?.name}->${state.name}",
                        kind = MemoryKind.GENERAL,
                        stress = env.thermalRatio > 0.85f,
                    )
                    lastState = state
                }
                delay(POLL_INTERVAL_MS)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        pollJob?.cancel()
        super.onDestroy()
    }

    companion object {
        private const val POLL_INTERVAL_MS = 45_000L

        fun start(context: Context) {
            context.startService(Intent(context, SensorPollingService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SensorPollingService::class.java))
        }
    }
}
