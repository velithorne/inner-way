package com.falcor.civilization.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.falcor.civilization.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FalcorMainScreen(viewModel: MainViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("QUEUE", "ARENA", "LEDGER", "REPORTS")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FALCOR-CIVILIZATION") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    var expanded by remember { mutableStateOf(false) }
                    val projects = viewModel.projects.collectAsState().value
                    val selectedId = viewModel.selectedProjectId.collectAsState().value
                    Box {
                        TextButton(onClick = { expanded = true }) {
                            Text(projects.find { it.id == selectedId }?.name ?: "Select Project")
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            projects.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.name) },
                                    onClick = {
                                        viewModel.selectProject(p.id)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val cycleState = viewModel.cycleState.collectAsState().value
                Button(
                    onClick = { viewModel.runDemoCycle() },
                    enabled = cycleState is com.falcor.civilization.engine.orchestrator.CycleOrchestrator.CycleState.Idle
                ) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Demo Cycle")
                }
                if (cycleState is com.falcor.civilization.engine.orchestrator.CycleOrchestrator.CycleState.Running) {
                    OutlinedButton(onClick = { viewModel.stopCycle() }) {
                        Text("Stop")
                    }
                    Text(
                        cycleState.phase,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                if (cycleState is com.falcor.civilization.engine.orchestrator.CycleOrchestrator.CycleState.Completed) {
                    Text(
                        cycleState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> com.falcor.civilization.ui.tabs.QueueTab(viewModel)
                1 -> com.falcor.civilization.ui.tabs.ArenaTab(viewModel)
                2 -> com.falcor.civilization.ui.tabs.LedgerTab(viewModel)
                3 -> com.falcor.civilization.ui.tabs.ReportsTab(viewModel)
            }
        }
    }
}
