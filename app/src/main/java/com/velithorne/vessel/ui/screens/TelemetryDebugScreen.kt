package com.velithorne.vessel.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.velithorne.vessel.telemetry.NetworkTransport
import com.velithorne.vessel.telemetry.TelemetrySnapshot
import com.velithorne.vessel.ui.components.MetricCard
import com.velithorne.vessel.ui.components.SectionHeader
import com.velithorne.vessel.ui.components.StatusChip
import com.velithorne.vessel.util.Formatters
import com.velithorne.vessel.viewmodel.TelemetryViewModel

/**
 * Live diagnostic surface — feeds the future physiology / organ-mapping spine.
 *
 * TODO Phase 2: overlay organ stress glyphs sourced from [com.velithorne.vessel.domain.phase2.PhysiologyEngine].
 */
@Composable
fun TelemetryDebugScreen(
    viewModel: TelemetryViewModel,
    modifier: Modifier = Modifier,
) {
    val snapshot by viewModel.telemetry.collectAsState()
    val scroll = rememberScrollState()
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            HeaderRow(snapshot)
            DeviceSection(snapshot)
            PowerSection(snapshot)
            StorageSection(snapshot)
            MemorySection(snapshot)
            NetworkSection(snapshot)
            MotionSection(snapshot)
            SystemSection(snapshot)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeaderRow(snapshot: TelemetrySnapshot) {
    Column {
        Text(
            text = "Velithorne Vessel",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Telemetry spine · phase 1",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
            StatusChip(label = "LIVE", highlight = true)
        }
        Text(
            text = "Last tick: ${Formatters.formatTimestampMillis(snapshot.timestampMillis)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun DeviceSection(s: TelemetrySnapshot) {
    MetricCard {
        SectionHeader(
            title = "Device",
            subtitle = "Static substrate · evolution seeds attach here later",
        )
        metricLine("Model", s.deviceModel)
        metricLine("Android", s.androidVersion)
        metricLine("Uptime", Formatters.formatDurationMs(s.uptimeMillis))
    }
}

@Composable
private fun PowerSection(s: TelemetrySnapshot) {
    MetricCard {
        SectionHeader(
            title = "Power",
            subtitle = "Metabolic proxy for future organ load mapping",
        )
        metricLine("Battery", Formatters.formatPercent(s.batteryPct))
        metricLine("Charging", Formatters.yesNo(s.isCharging))
        metricLine("Battery temp", Formatters.formatCelsius(s.batteryTempC))
        metricLine("Power saver", Formatters.yesNo(s.powerSaveEnabled))
    }
}

@Composable
private fun StorageSection(s: TelemetrySnapshot) {
    MetricCard {
        SectionHeader(
            title = "Storage",
            subtitle = "Growth / archiving budgets (Room persistence hooks nearby)",
        )
        metricLine("Used", Formatters.formatBytes(s.storageUsedBytes))
        metricLine("Free", Formatters.formatBytes(s.storageFreeBytes))
        metricLine("Used %", Formatters.formatPercent(s.storageUsedPct))
    }
}

@Composable
private fun MemorySection(s: TelemetrySnapshot) {
    MetricCard {
        SectionHeader(
            title = "Memory",
            subtitle = "Host memory class · coarse low-RAM hint only",
        )
        val classStr = s.memoryClassMb?.let { "$it MB" } ?: Formatters.unavailable()
        metricLine("Memory class", classStr)
        metricLine("Low-ram device", Formatters.yesNo(s.lowMemoryFlag))
    }
}

@Composable
private fun NetworkSection(s: TelemetrySnapshot) {
    MetricCard {
        SectionHeader(
            title = "Network",
            subtitle = "Environmental connectivity — signal organ analogs later",
        )
        metricLine("Connected", Formatters.yesNo(s.networkConnected))
        metricLine("Type", networkTypeLabel(s))
        metricLine("Metered", if (s.networkMetered == null) Formatters.unavailable() else Formatters.yesNo(s.networkMetered))
    }
}

@Composable
private fun MotionSection(s: TelemetrySnapshot) {
    MetricCard {
        SectionHeader(
            title = "Motion / sensors",
            subtitle = "Kinematics + photic intake",
        )
        metricLine("Motion intensity", Formatters.formatMotionIntensity(s.motionIntensity))
        metricLine("Pitch", Formatters.formatDegrees(s.orientationPitchDeg))
        metricLine("Roll", Formatters.formatDegrees(s.orientationRollDeg))
        metricLine("Ambient light", Formatters.formatLux(s.ambientLightLux))
    }
}

@Composable
private fun SystemSection(s: TelemetrySnapshot) {
    MetricCard {
        SectionHeader(
            title = "System",
            subtitle = "VesselRenderer will read interaction state for presentation policy",
        )
        metricLine("Screen interactive", Formatters.yesNo(s.screenInteractive))
        metricLine("Snapshot time", Formatters.formatTimestampMillis(s.timestampMillis))
    }
}

@Composable
private fun metricLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun networkTypeLabel(s: TelemetrySnapshot): String {
    return when (s.networkType) {
        NetworkTransport.WIFI -> "Wi-Fi"
        NetworkTransport.CELLULAR -> "Mobile"
        NetworkTransport.ETHERNET -> "Ethernet"
        NetworkTransport.VPN -> "VPN"
        NetworkTransport.BLUETOOTH -> "Bluetooth"
        NetworkTransport.NONE -> if (s.networkConnected == true) "Other" else "None"
        NetworkTransport.OTHER -> "Other"
    }
}
