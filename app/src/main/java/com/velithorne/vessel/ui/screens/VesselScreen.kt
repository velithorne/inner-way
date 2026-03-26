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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.velithorne.vessel.physiology.OrganType
import com.velithorne.vessel.renderer.RenderTuning
import com.velithorne.vessel.renderer.VesselGestureController
import com.velithorne.vessel.renderer.VesselScene
import com.velithorne.vessel.ui.components.GrowthStatusChip
import com.velithorne.vessel.ui.components.VesselControlChip
import com.velithorne.vessel.ui.components.VesselLegendChip
import com.velithorne.vessel.ui.components.VesselOrganSheet
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
    val selected by viewModel.selectedVesselOrgan.collectAsState()
    val inspection by viewModel.organInspection.collectAsState()
    val sheetVisible by viewModel.vesselSheetVisible.collectAsState()
    val growth by viewModel.growthVisual.collectAsState()
    var overlayExpanded by rememberSaveable { mutableStateOf(false) }

    val tuning = remember { RenderTuning() }
    val gestureController = remember(tuning) { VesselGestureController(tuning) }

    var chamberOffset by remember { mutableStateOf(Offset.Zero) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Observation chamber",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        // Avoid Row+weight squeezing text to 0 width (per-character wrap glitch when sheet opens).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 3.dp, bottom = 6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Specimen 01 · seed-stage morphogenesis · pinch · pan · tilt",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
                    modifier = Modifier.weight(1f),
                )
                GrowthStatusChip(
                    label = growth.statusLabel,
                    subtitle = null,
                )
            }
            if (growth.statusLine.isNotEmpty()) {
                Text(
                    text = growth.statusLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coords ->
                        chamberOffset = coords.positionInRoot()
                    },
            ) {
                VesselScene(
                    physiology = physiology,
                    scene = scene,
                    selectedOrgan = selected,
                    gestureController = gestureController,
                    renderOffset = chamberOffset,
                    onSelectOrgan = { organ ->
                        viewModel.selectVesselOrgan(organ)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VesselControlChip(
                label = "Reset view",
                onClick = {
                    gestureController.requestResetNextFrame()
                    viewModel.selectVesselOrgan(null)
                },
            )
            VesselControlChip(
                label = if (sheetVisible) "Hide detail" else "Anatomy",
                onClick = {
                    if (selected != null) {
                        viewModel.showVesselSheet(!sheetVisible)
                    }
                },
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        VesselStatusOverlay(
            stateLabel = scene.healthLabel,
            vitality = scene.vitalityDisplay,
            feverLabel = scene.feverLabel,
            hungerLabel = scene.hungerLabel,
            selectedOrganName = selected?.let { organDisplayName(it) },
            statusLine = scene.statusLine,
            growthHint = growth.snapshot.explainerLines.firstOrNull(),
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

    VesselOrganSheet(
        inspection = inspection,
        visible = sheetVisible && selected != null,
        onDismiss = { viewModel.dismissVesselSheet() },
    )
}

private fun organDisplayName(type: OrganType): String = when (type) {
    OrganType.METABOLIC_HEART -> "Metabolic Heart"
    OrganType.CORTEX_CLUSTER -> "Cortex Cluster"
    OrganType.NEURAL_GEL -> "Neural Gel"
    OrganType.ARCHIVE_VAULT -> "Archive Vault"
    OrganType.SIGNAL_LUNGS -> "Signal Lungs"
    OrganType.VESTIBULAR_MUSCULATURE -> "Vestibular Musculature"
    OrganType.THERMAL_MEMBRANE -> "Thermal Membrane"
}
