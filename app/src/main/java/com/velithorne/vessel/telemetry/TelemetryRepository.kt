package com.velithorne.vessel.telemetry

import android.app.Application
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.velithorne.vessel.core.Constants
import com.velithorne.vessel.core.TimeProvider
import com.velithorne.vessel.util.Smoothing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Aggregates monitors into a single [TelemetrySnapshot] stream.
 *
 * Phase 2+: [com.velithorne.vessel.physiology.PhysiologyEngine] consumes [snapshot].
 * Later: persist windows via Room; VesselRenderer as read-only consumer.
 */
class TelemetryRepository(
    application: Application,
    private val timeProvider: TimeProvider,
) : DefaultLifecycleObserver {

    private val appContext: Application = application
    private val batteryMonitor = BatteryMonitor(application)
    private val storageMonitor = StorageMonitor(application)
    private val memoryMonitor = MemoryMonitor(application)
    private val networkMonitor = NetworkMonitor(application)
    private val sensorMonitor = SensorMonitor(application)
    private val motionMonitor = MotionMonitor()
    private val deviceInfo = DeviceInfoProvider(timeProvider)
    private val powerManager: PowerManager? =
        ContextCompat.getSystemService(application, PowerManager::class.java)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null

    private val _snapshot = MutableStateFlow(emptySnapshot())
    val snapshot: StateFlow<TelemetrySnapshot> = _snapshot.asStateFlow()

    private var motionSmoothed: Float? = null

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        startSensorsAndTicker()
    }

    override fun onStop(owner: LifecycleOwner) {
        stopSensorsAndTicker()
    }

    /** Explicit lifecycle if tests need to drive collection without ProcessLifecycleOwner. */
    fun start() {
        startSensorsAndTicker()
    }

    fun stop() {
        stopSensorsAndTicker()
    }

    private fun startSensorsAndTicker() {
        if (tickJob?.isActive == true) return
        motionMonitor.reset()
        motionSmoothed = null
        sensorMonitor.start()
        tickJob = scope.launch {
            while (isActive) {
                emitSnapshot()
                delay(Constants.TELEMETRY_TICK_MS)
            }
        }
    }

    private fun stopSensorsAndTicker() {
        tickJob?.cancel()
        tickJob = null
        sensorMonitor.stop()
    }

    private suspend fun emitSnapshot() {
        val battery = batteryMonitor.read()
        val storage = storageMonitor.read()
        val memory = memoryMonitor.read()
        val network = networkMonitor.read()
        val sensorSnap = sensorMonitor.snapshot()
        val motion = motionMonitor.derive(sensorSnap)

        val rawIntensity = motion.rawMotionIntensity
        motionSmoothed = Smoothing.exponentialMovingAverage(
            previous = motionSmoothed,
            next = rawIntensity,
            alpha = Constants.MOTION_SMOOTHING_ALPHA,
        )

        val screenOn = try {
            powerManager?.isInteractive
        } catch (_: Throwable) {
            null
        }

        val ts = TelemetrySnapshot(
            timestampMillis = timeProvider.currentTimeMillis(),
            batteryPct = battery.batteryPct,
            isCharging = battery.isCharging,
            batteryTempC = battery.batteryTempC,
            powerSaveEnabled = battery.powerSaveEnabled,
            storageUsedBytes = storage.usedBytes,
            storageFreeBytes = storage.freeBytes,
            storageUsedPct = storage.usedPct,
            memoryClassMb = memory.memoryClassMb,
            lowMemoryFlag = memory.lowMemoryFlag,
            lowRamDevice = memory.lowRamDevice,
            networkConnected = network.connected,
            networkType = network.transport,
            networkMetered = network.metered,
            motionIntensity = motionSmoothed,
            orientationPitchDeg = motion.pitchDeg,
            orientationRollDeg = motion.rollDeg,
            ambientLightLux = motion.ambientLux,
            screenInteractive = screenOn,
            uptimeMillis = deviceInfo.uptimeMillis(),
            deviceModel = deviceInfo.deviceModel,
            androidVersion = deviceInfo.androidVersion,
        )
        _snapshot.value = ts
    }

    private fun emptySnapshot(): TelemetrySnapshot = TelemetrySnapshot(
        timestampMillis = timeProvider.currentTimeMillis(),
        batteryPct = null,
        isCharging = null,
        batteryTempC = null,
        powerSaveEnabled = null,
        storageUsedBytes = null,
        storageFreeBytes = null,
        storageUsedPct = null,
        memoryClassMb = null,
        lowMemoryFlag = null,
        lowRamDevice = null,
        networkConnected = null,
        networkType = NetworkTransport.NONE,
        networkMetered = null,
        motionIntensity = null,
        orientationPitchDeg = null,
        orientationRollDeg = null,
        ambientLightLux = null,
        screenInteractive = null,
        uptimeMillis = deviceInfo.uptimeMillis(),
        deviceModel = deviceInfo.deviceModel,
        androidVersion = deviceInfo.androidVersion,
    )

    /**
     * TODO: Clear coroutine scope if repository becomes short-lived (e.g. tests).
     * Process-lifetime repository does not need onDestroy today.
     */
    fun shutdownForTests() {
        stopSensorsAndTicker()
        scope.cancel()
    }
}
