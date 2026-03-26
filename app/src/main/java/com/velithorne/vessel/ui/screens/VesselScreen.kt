package com.velithorne.vessel.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.velithorne.vessel.physiology.OrganType
import com.velithorne.vessel.renderer.VesselScene
import com.velithorne.vessel.ui.components.VesselLegendChip
import com.velithorne.vessel.ui.components.VesselStatusOverlay
import com.velithorne.vessel.util.Formatters
import com.velithorne.vessel.viewmodel.TelemetryViewModel

@Composable
fun VesselScreen(
    viewModel: TelemetryViewModel,
    modifier: Modifier = Modifier,
) {
    val physiology by viewModel.physiology.collectAsState()
    val scene by viewModel.vesselScene.collectAsState()
    var overlayExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Observation chamber",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Specimen 01 · live synthesis",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            VesselScene(
                physiology = physiology,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        VesselStatusOverlay(
            stateLabel = scene.healthLabel,
            vitality = scene.vitalityDisplay,
            feverLabel = scene.feverLabel,
            hungerLabel = scene.hungerLabel,
            statusLine = scene.statusLine,
            expanded = overlayExpanded,
            onToggleInfo = { overlayExpanded = !overlayExpanded },
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            physiology.organs.distinctBy { it.organType }.forEach { o ->
                val short = when (o.organType) {
                    OrganType.METABOLIC_HEART -> "Heart"
                    OrganType.CORTEX_CLUSTER -> "Cortex"
                    OrganType.NEURAL_GEL -> "Gel"
                    OrganType.ARCHIVE_VAULT -> "Vault"
                    OrganType.SIGNAL_LUNGS -> "Lungs"
                    OrganType.VESTIBULAR_MUSCULATURE -> "Vestibular"
                    OrganType.THERMAL_MEMBRANE -> "Thermal"
                }
                VesselLegendChip(label = "$short · ${Formatters.formatUnitInterval(o.health)}")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
