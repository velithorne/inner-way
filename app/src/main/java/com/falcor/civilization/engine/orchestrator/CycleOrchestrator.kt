package com.falcor.civilization.engine.orchestrator

import com.falcor.civilization.data.FalcorDatabase
import com.falcor.civilization.data.dao.*
import com.falcor.civilization.data.entities.*
import com.falcor.civilization.domain.LedgerPayload
import com.falcor.civilization.domain.PreregPlanContent
import com.falcor.civilization.engine.agents.*
import com.falcor.civilization.engine.analysis.AnalysisPipeline
import com.falcor.civilization.engine.ledger.LedgerService
import com.falcor.civilization.engine.sim.ArtifactInjector
import com.falcor.civilization.engine.sim.Simulator
import com.falcor.civilization.engine.util.HashUtil
import com.falcor.civilization.util.IdGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class CycleOrchestrator(
    private val db: FalcorDatabase,
    private val ledgerService: LedgerService,
    private val filesDir: File
) {
    private val json = Json { prettyPrint = false }

    private val _cycleState = MutableStateFlow<CycleState>(CycleState.Idle)
    val cycleState: StateFlow<CycleState> = _cycleState.asStateFlow()

    private val _arenaEvents = MutableStateFlow<List<ArenaEvent>>(emptyList())
    val arenaEvents: StateFlow<List<ArenaEvent>> = _arenaEvents.asStateFlow()

    private var cycleJob: kotlinx.coroutines.Job? = null

    data class ArenaEvent(
        val runId: String,
        val artifactInjected: Boolean,
        val detectorTriggered: Boolean,
        val auditorCaught: Boolean,
        val timestamp: Long
    )

    sealed class CycleState {
        data object Idle : CycleState()
        data class Running(val phase: String, val progress: Int, val total: Int) : CycleState()
        data class Completed(val message: String) : CycleState()
        data class Error(val message: String) : CycleState()
    }

    suspend fun runDemoCycle(projectId: String) = withContext(Dispatchers.IO) {
        _cycleState.value = CycleState.Running("Initializing", 0, 5)
        val seed = 42L
        val scientist = ScientistAgent(seed)
        val skeptic = SkepticAgent(seed + 1)
        val auditor = AuditorAgent()

        val hypothesis = scientist.proposeHypothesis(projectId)
        db.hypothesisDao().insert(HypothesisEntity(
            id = hypothesis.id,
            projectId = hypothesis.projectId,
            title = hypothesis.title,
            description = hypothesis.description,
            createdAt = System.currentTimeMillis(),
            status = "PREREGISTERED"
        ))

        val prereg = scientist.generatePreregPlan(hypothesis.id, hypothesis.primaryMetrics)
        val planHash = HashUtil.sha256(prereg.planJson)
        db.preregPlanDao().insert(PreregPlanEntity(
            id = prereg.id,
            hypothesisId = prereg.hypothesisId,
            planJson = prereg.planJson,
            planHash = planHash,
            frozenAt = System.currentTimeMillis(),
            isFrozen = true
        ))

        ledgerService.append(LedgerPayload(
            type = "PREREG_FREEZE",
            entityId = prereg.id,
            data = planHash,
            timestamp = System.currentTimeMillis()
        ))

        _cycleState.value = CycleState.Running("Running simulations", 1, 5)
        val configs = scientist.generateExperimentConfigs(prereg.id, 3)
        configs.forEach { cfg ->
            db.experimentConfigDao().insert(ExperimentConfigEntity(
                id = cfg.id,
                preregPlanId = cfg.preregPlanId,
                seed = cfg.seed,
                simScenario = cfg.simScenario.id,
                sensorSet = json.encodeToString(cfg.sensorSet),
                controlParamsJson = "{}",
                createdAt = System.currentTimeMillis()
            ))
        }

        val artifactMap = skeptic.selectArtifactAttacks(configs.size)
        val runResults = mutableListOf<Pair<RunEntity, PipelineResult>>()

        for ((i, cfg) in configs.withIndex()) {
            val runId = IdGenerator.generate()
            val artifacts = artifactMap[i]
            val injector = artifacts?.let { skeptic.createInjector(cfg.seed + 100, it) }
            val sim = Simulator(
                seed = cfg.seed,
                scenario = cfg.simScenario,
                sampleRateHz = cfg.sensorSet.sampleRateHz,
                durationSec = 20.0,
                artifactInjector = injector
            )
            val observations = mutableListOf<com.falcor.civilization.domain.ObservationPoint>()
            sim.stream().collect { observations.add(it) }

            val runDir = File(filesDir, "projects/$projectId/runs/$runId")
            runDir.mkdirs()
            File(runDir, "raw.csv").writeText(
                "t," + observations.first().channels.keys.joinToString(",") + "\n" +
                        observations.take(100).joinToString("\n") { o ->
                            o.t.toString() + "," + o.channels.values.joinToString(",")
                        }
            )

            db.runDao().insert(RunEntity(
                id = runId,
                projectId = projectId,
                configId = cfg.id,
                startedAt = System.currentTimeMillis(),
                endedAt = System.currentTimeMillis(),
                mode = "SIM",
                blind = true,
                status = "COMPLETED"
            ))

            artifacts?.forEach { spec ->
                db.runArtifactDao().insert(RunArtifactEntity(
                    id = IdGenerator.generate(),
                    runId = runId,
                    artifactType = spec.type,
                    paramsJson = json.encodeToString(spec),
                    injectedByAgent = "SkepticAgent",
                    createdAt = System.currentTimeMillis()
                ))
            }

            val pipeline = AnalysisPipeline(cfg.seed, prereg.content)
            val result = pipeline.run(observations)

            db.analysisResultDao().insert(AnalysisResultEntity(
                id = IdGenerator.generate(),
                runId = runId,
                metricsJson = json.encodeToString(result.metrics),
                pValuesJson = json.encodeToString(result.rawPValues),
                posteriorJson = json.encodeToString(mapOf("posteriorOdds" to result.posteriorOdds)),
                correctionJson = json.encodeToString(result.correctedPValues),
                decision = result.decision,
                createdAt = System.currentTimeMillis()
            ))

            _arenaEvents.value = _arenaEvents.value + ArenaEvent(
                runId = runId,
                artifactInjected = artifacts != null,
                detectorTriggered = result.decision == "PROMOTE",
                auditorCaught = !result.integrityPassed,
                timestamp = System.currentTimeMillis()
            )

            runResults.add(Pair(runId, result))
        }

        _cycleState.value = CycleState.Running("Evaluating findings", 4, 5)
        val promoted = runResults.filter { (_, r) -> r.decision == "PROMOTE" && r.integrityPassed }
        val artifactRuns = runResults.filter { (runId, _) ->
            db.runArtifactDao().getByRun(runId).isNotEmpty()
        }
        val artifactCaught = artifactRuns.count { (_, r) -> !r.integrityPassed || r.decision != "PROMOTE" }

        if (promoted.isNotEmpty()) {
            val (_, res) = promoted.first()
            db.findingDao().insert(FindingEntity(
                id = IdGenerator.generate(),
                hypothesisId = hypothesis.id,
                summary = "Effect detected: ${res.metrics.keys.firstOrNull() ?: "unknown"}",
                evidenceScore = res.posteriorOdds,
                replicationScore = 0.5,
                promotedAt = System.currentTimeMillis(),
                retiredAt = null
            ))
        }

        _cycleState.value = CycleState.Completed(
            "Demo complete. ${promoted.size} finding(s) promoted. " +
                    "Artifact runs: ${artifactRuns.size}, caught: $artifactCaught"
        )
    }

    fun stopCycle() {
        cycleJob?.cancel()
        _cycleState.value = CycleState.Idle
    }
}
