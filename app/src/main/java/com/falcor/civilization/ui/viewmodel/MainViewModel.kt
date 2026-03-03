package com.falcor.civilization.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.falcor.civilization.FalcorApp
import com.falcor.civilization.data.FalcorDatabase
import com.falcor.civilization.data.FalcorDatabaseProvider
import com.falcor.civilization.data.entities.ProjectEntity
import com.falcor.civilization.engine.ledger.LedgerService
import com.falcor.civilization.engine.orchestrator.CycleOrchestrator
import com.falcor.civilization.engine.reports.ExportService
import com.falcor.civilization.util.IdGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as? FalcorApp)?.database
        ?: FalcorDatabaseProvider.get(application)
    private val filesDir = (application as? FalcorApp)?.appFilesDir
        ?: application.getExternalFilesDir(null) ?: File(application.filesDir, "falcor")
    private val ledgerFile = File(filesDir, "ledger.jsonl")
    private val ledgerService = LedgerService(db.ledgerEntryDao(), ledgerFile)
    val orchestrator = CycleOrchestrator(db, ledgerService, filesDir)
    private val exportService = ExportService(
        db.runDao(), db.runArtifactDao(), db.analysisResultDao(),
        db.experimentConfigDao(), db.preregPlanDao(), ledgerService,
        com.falcor.civilization.engine.reports.ReportGenerator(), filesDir
    )

    val projects: StateFlow<List<ProjectEntity>> = db.projectDao().getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedProjectId = MutableStateFlow<String?>(null)
    val runs = selectedProjectId.flatMapLatest { pid ->
        pid?.let { db.runDao().getByProject(it) } ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ledgerEntries = db.ledgerEntryDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val findings = db.findingDao().getAllActiveFindings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cycleState = orchestrator.cycleState
    val arenaEvents = orchestrator.arenaEvents

    private val _ledgerVerificationResult = MutableStateFlow<com.falcor.civilization.engine.ledger.LedgerVerificationResult?>(null)
    val ledgerVerificationResult: StateFlow<com.falcor.civilization.engine.ledger.LedgerVerificationResult?> = _ledgerVerificationResult.asStateFlow()

    init {
        viewModelScope.launch {
            if (db.projectDao().getById("default") == null) {
                db.projectDao().insert(ProjectEntity(
                    id = "default",
                    name = "Falcor Device",
                    createdAt = System.currentTimeMillis()
                ))
                selectedProjectId.value = "default"
            } else {
                selectedProjectId.value = selectedProjectId.value ?: "default"
            }
        }
    }

    fun selectProject(id: String) {
        selectedProjectId.value = id
    }

    fun runDemoCycle() {
        viewModelScope.launch {
            orchestrator.runDemoCycle(selectedProjectId.value ?: "default")
        }
    }

    fun stopCycle() {
        orchestrator.stopCycle()
    }

    fun verifyLedger() {
        viewModelScope.launch {
            _ledgerVerificationResult.value = ledgerService.verifyChain()
        }
    }

    fun exportRun(runId: String) {
        viewModelScope.launch {
            val exportDir = File(filesDir, "exports")
            exportDir.mkdirs()
            val outputFile = File(exportDir, "${runId}_bundle.zip")
            exportService.exportReproBundle(runId, selectedProjectId.value ?: "default", outputFile)
        }
    }
}
