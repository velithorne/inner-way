package com.velithorne.vessel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.velithorne.vessel.ui.screens.VesselMainScreen
import com.velithorne.vessel.ui.theme.VelithorneVesselTheme
import com.velithorne.vessel.ui.theme.VesselBg
import com.velithorne.vessel.viewmodel.TelemetryViewModel
import com.velithorne.vessel.viewmodel.TelemetryViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        val container = (application as VesselApplication).appContainer
        val factory = TelemetryViewModelFactory(
            repository = container.telemetryRepository,
            physiologyEngine = container.physiologyEngine,
            vesselRenderer = container.vesselRenderer,
            morphogenesisEngine = container.morphogenesisEngine,
        )
        setContent {
            VelithorneVesselTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(VesselBg),
                ) {
                    val vm: TelemetryViewModel = viewModel(factory = factory)
                    VesselMainScreen(viewModel = vm)
                }
            }
        }
    }
}
