package com.falcor.civilization.engine.hardware

import com.falcor.civilization.domain.ObservationPoint
import kotlinx.coroutines.flow.Flow

/**
 * Interface for hardware sensor streams. Implement for serial/BLE integration later.
 * SIM mode uses SimStreamProvider instead.
 */
interface HardwareStreamProvider {
    fun streamObservations(runId: String, sampleRateHz: Double): Flow<ObservationPoint>
    suspend fun connect(): Boolean
    suspend fun disconnect()
}
