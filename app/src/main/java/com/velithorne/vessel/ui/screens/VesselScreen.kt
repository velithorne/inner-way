package com.velithorne.vessel.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.velithorne.vessel.physiology.OrganType
import com.velithorne.vessel.renderer_seedpod.SeedPodGestureController
import com.velithorne.vessel.renderer_seedpod.SeedPodScene
import com.velithorne.vessel.renderer_seedpod.SeedPodTuning
import com.velithorne.vessel.ui.components.GrowthProgressCard
import com.velithorne.vessel.ui.components.LiveConditionChip
import com.velithorne.vessel.ui.components.StageStatusChip
import com.velithorne.vessel.ui.components.ReturnGrowthSummarySheet
import com.velithorne.vessel.ui.components.SeedPodReturnSummarySheet
import com.velithorne.vessel.ui.components.VesselControlChip
import com.velithorne.vessel.ui.components.VesselLegendChip
import com.velithorne.vessel.ui.components.VesselOrganSheet
import com.velithorne.vessel.ui.components.VesselStatusOverlay
import com.velithorne.vessel.util.Formatters
import com.velithorne.vessel.viewmodel.TelemetryViewModel

/**
 * Set to `true` temporarily to tint/border progress, chamber viewport, and status card
 * and verify top-to-bottom layout order (disable before release).
 */
private const val DEBUG_VESSEL_LAYOUT_BORDERS = false

@Composable
private fun Modifier.vesselDebugBorder(
    enabled: Boolean,
    color: Color,
): Modifier {
    if (!enabled) return this
    return border(width = 2.dp, color = color)
}

/**
 * Vessel tab: [SeedPodScene] only.
 *
 * **Layout:** Single [Column] + [verticalScroll] in strict document order — progress card, then
 * fixed-height chamber (no [LazyColumn] weight distribution), then actions and status below.
 */
@Composable
fun VesselScreen(
    viewModel: TelemetryViewModel,
    modifier: Modifier = Modifier,
    onOpenLineage: () -> Unit = {},
) {
    val physiology by viewModel.physiology.collectAsState()
    val scene by viewModel.seedPodScene.collectAsState()
    val selectedOrgan by viewModel.selectedVesselOrgan.collectAsState()
    val selectedPod by viewModel.selectedSeedPodTarget.collectAsState()
    val inspection by viewModel.organInspection.collectAsState()
    val sheetVisible by viewModel.vesselSheetVisible.collectAsState()
    val podUi by viewModel.seedPodVesselUi.collectAsState()
    val returnSummaryFlow by viewModel.growthReturnSummary.collectAsState()
    val seedPodReturn by viewModel.seedPodReturnSummary.collectAsState()
    var showReturnSummarySheet by rememberSaveable { mutableStateOf(false) }
    var showSeedPodReturnSheet by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(returnSummaryFlow) {
        val s = returnSummaryFlow
        showReturnSummarySheet = s != null && s.deltas.isNotEmpty()
    }
    LaunchedEffect(seedPodReturn) {
        showSeedPodReturnSheet = seedPodReturn != null && seedPodReturn!!.lines.isNotEmpty()
    }
    var overlayExpanded by rememberSaveable { mutableStateOf(false) }

    val tuning = remember { SeedPodTuning() }
    val gestureController = remember(tuning) { SeedPodGestureController(tuning) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Silicon seed chamber",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Lineage",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onOpenLineage() }
                    .padding(start = 8.dp),
            )
        }
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
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LiveConditionChip(label = podUi.liveConditionLabel)
                StageStatusChip(structuralStageLabel = podUi.structuralStageLabel)
            }
            if (podUi.statusLine.isNotEmpty()) {
                Text(
                    text = podUi.statusLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (podUi.branchStatusLine.isNotEmpty()) {
                Text(
                    text = podUi.branchStatusLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (podUi.branchReasonLine.isNotEmpty()) {
                Text(
                    text = podUi.branchReasonLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (podUi.ambientEcologyHintLine.isNotEmpty()) {
                Text(
                    text = podUi.ambientEcologyHintLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.42f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        GrowthProgressCard(
            progress = podUi.growthProgressFraction,
            progressCaption = podUi.progressCaption,
            activeBudgetChannelLabel = podUi.activeBudgetChannelLabel,
            recentAwayLine = podUi.recentAwayLine,
            liveStrainIndicator = podUi.liveStrainIndicator,
            modifier = Modifier
                .padding(top = 8.dp)
                .vesselDebugBorder(
                    DEBUG_VESSEL_LAYOUT_BORDERS,
                    Color.Magenta.copy(alpha = 0.85f),
                ),
        )
        // Tight gap: viewport starts immediately under developmental progress (no lazy-column slack).
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .vesselDebugBorder(
                    DEBUG_VESSEL_LAYOUT_BORDERS,
                    Color.Cyan.copy(alpha = 0.9f),
                ),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.22f),
            tonalElevation = 0.dp,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
            ) {
                SeedPodScene(
                    physiology = physiology,
                    scene = scene,
                    selectedTarget = selectedPod,
                    gestureController = gestureController,
                    onSelectTarget = { viewModel.selectSeedPodTarget(it) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
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
            modifier = Modifier
                .padding(top = 10.dp)
                .vesselDebugBorder(
                    DEBUG_VESSEL_LAYOUT_BORDERS,
                    Color.Yellow.copy(alpha = 0.9f),
                ),
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
        Spacer(modifier = Modifier.height(16.dp))
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

    SeedPodReturnSummarySheet(
        summary = seedPodReturn,
        visible = showSeedPodReturnSheet,
        onDismiss = {
            showSeedPodReturnSheet = false
            viewModel.dismissSeedPodReturnSummary()
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
