package com.velithorne.vessel.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.velithorne.vessel.ui.theme.VesselBg
import com.velithorne.vessel.viewmodel.TelemetryViewModel

/**
 * Phase 3: **Vessel** is the primary tab; raw telemetry and physiology remain available.
 */
@Composable
fun VesselMainScreen(
    viewModel: TelemetryViewModel,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    /** Bump when user **returns** to Vessel from another tab (fresh gesture state + canvas). */
    var vesselSession by rememberSaveable { mutableIntStateOf(0) }
    var prevTab by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(tab) {
        if (tab == 0 && prevTab != 0) vesselSession++
        prevTab = tab
    }
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = VesselBg,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TabRow(
                selectedTabIndex = tab,
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                contentColor = MaterialTheme.colorScheme.onBackground,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[tab]),
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            ) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = { Text("Vessel") },
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text("Telemetry") },
                )
                Tab(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    text = { Text("Physiology") },
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = 4.dp),
        ) {
            when (tab) {
                // Fresh chamber + camera each time user returns to Vessel (clear stray zoom/pan/tilt).
                0 -> key(vesselSession) { VesselScreen(viewModel = viewModel) }
                1 -> TelemetryDebugScreen(viewModel = viewModel)
                2 -> PhysiologyDebugScreen(viewModel = viewModel)
            }
        }
    }
}
