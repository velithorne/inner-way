package com.velithorne.vessel.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.velithorne.vessel.morphogenesis.MorphogenesisEngine
import com.velithorne.vessel.physiology.PhysiologyEngine
import com.velithorne.vessel.renderer.VesselRenderer
import com.velithorne.vessel.telemetry.TelemetryRepository

class TelemetryViewModelFactory(
    private val repository: TelemetryRepository,
    private val physiologyEngine: PhysiologyEngine,
    private val vesselRenderer: VesselRenderer,
    private val morphogenesisEngine: MorphogenesisEngine,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TelemetryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TelemetryViewModel(repository, physiologyEngine, vesselRenderer, morphogenesisEngine) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
