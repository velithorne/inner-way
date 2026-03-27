package com.velithorne.vessel.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.velithorne.vessel.renderer_seedpod.SeedPodGestureController
import com.velithorne.vessel.renderer_seedpod.SeedPodScene
import com.velithorne.vessel.renderer_seedpod.SeedPodTuning
import com.velithorne.vessel.ui.components.GrowthProgressCard
import com.velithorne.vessel.ui.components.ReturnGrowthSummarySheet
import com.velithorne.vessel.ui.components.VesselControlChip
import com.velithorne.vessel.ui.components.VesselLegendChip
import com.velithorne.vessel.ui.components.VesselStageChip
import com.velithorne.vessel.ui.components.VesselOrganSheet
import com.velithorne.vessel.ui.components.VesselStatusOverlay
import com.velithorne.vessel.util.Formatters
import com.velithorne.vessel.viewmodel.TelemetryViewModel

/**
 * Vessel tab: [SeedPodScene] only.
 *
 * **Layout:** Header + progress (wrap) → **chamber [weight(1f)]** fills the **entire middle** of the
 * tab (organism sits **under** progress in the **upper-mid** of that region via [SeedPodLayout]) →
 * controls + overlay + chips **wrap height at bottom** — no [weight] on the bottom block, so no
 * huge empty band between progress and the organism.
 */
@Composable
fun VesselScreen(
    viewModel: TelemetryViewModel,
    modifier: Modifier = Modifier,
) {
    val physiology by viewModel.physiology.collectAsState()
    val scene by viewModel.seedPodScene.collectAsState()
    val selectedOrgan by viewModel.selectedVesselOrgan.collectAsState()
    val selectedPod by viewModel.selectedSeedPodTarget.collectAsState()
    val inspection by viewModel.organInspection.collectAsState()
    val sheetVisible by viewModel.vesselSheetVisible.collectAsState()
    val podUi by viewModel.seedPodVesselUi.collectAsState()
    val returnSummaryFlow by viewModel.growthReturnSummary.collectAsState()
    var showReturnSummarySheet by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(returnSummaryFlow) {
        val s = returnSummaryFlow
        showReturnSummarySheet = s != null && s.deltas.isNotEmpty()
    }
    var overlayExpanded by rememberSaveable { mutableStateOf(false) }

    val tuning = remember { SeedPodTuning() }
    val gestureController = remember(tuning) { SeedPodGestureController(tuning) }

    var chamberOffset by remember { mutableStateOf(Offset.Zero) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Silicon seed chamber",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 3.dp, bottom = 4.dp),
        ) {
            Text(
                text = "Specimen 01 · seed pod · pinch · pan · tilt · tap pod for readout",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.weight(1f))
                VesselStageChip(
                    label = podUi.stageLabel,
                    modifier = Modifier.widthIn(max = 200.dp),
                )
            }
            if (podUi.statusLine.isNotEmpty()) {
                Text(
                    text = podUi.statusLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            GrowthProgressCard(
                progress = podUi.growthProgressFraction,
                activeBudgetChannelLabel = podUi.activeBudgetChannelLabel,
                recentAwayLine = podUi.recentAwayLine,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        // Fills **all space** between progress and bottom chrome — organism is the main viewport (upper-mid in canvas).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coords ->
                        chamberOffset = coords.positionInRoot()
                    },
            ) {
                SeedPodScene(
                    physiology = physiology,
                    scene = scene,
                    selectedTarget = selectedPod,
                    gestureController = gestureController,
                    renderOffset = chamberOffset,
                    onSelectTarget = { viewModel.selectSeedPodTarget(it) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // Wrap height only — does **not** expand to create a dead gap above the organism.
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VesselControlChip(
                    label = "Reset view",
                    onClick = {
                        gestureController.requestResetNextFrame()
                        viewModel.selectSeedPodTarget(null)
                    },
                )
                VesselControlChip(
                    label = if (sheetVisible) "Hide detail" else "Anatomy",
                    onClick = {
                        if (selectedOrgan != null) {
                            viewModel.showVesselSheet(!sheetVisible)
                        }
                    },
                )
            }
            VesselStatusOverlay(
                stateLabel = scene.physiology.species.healthLabel,
                vitality = scene.physiology.species.vitality,
                feverLabel = feverWord(scene.physiology.species.fever),
                hungerLabel = hungerWord(scene.physiology.species.hunger),
                selectedOrganName = selectedOrgan?.let { organDisplayName(it) },
                statusLine = scene.physiology.species.stateSummary,
                growthHint = podUi.statusLine.takeIf { it.isNotEmpty() },
                expanded = overlayExpanded,
                onToggleInfo = { overlayExpanded = !overlayExpanded },
            )
            Spacer(modifier = Modifier.height(6.dp))
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
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    VesselOrganSheet(
        inspection = inspection,
        visible = sheetVisible && selectedOrgan != null,
        onDismiss = { viewModel.dismissVesselSheet() },
    )

    ReturnGrowthSummarySheet(
        summary = returnSummaryFlow,
        visible = showReturnSummarySheet,
        onDismiss = {
            showReturnSummarySheet = false
            viewModel.dismissReturnGrowthSummary()
        },
    )
}

private fun feverWord(f: Float): String = when {
    f > 0.65f -> "High"
    f > 0.38f -> "Elevated"
    f > 0.15f -> "Mild"
    else -> "Norm"
}

private fun hungerWord(h: Float): String = when {
    h > 0.7f -> "Severe"
    h > 0.45f -> "Moderate"
    h > 0.22f -> "Light"
    else -> "Satiated"
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
