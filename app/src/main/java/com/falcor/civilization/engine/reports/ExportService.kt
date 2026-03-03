package com.falcor.civilization.engine.reports

import com.falcor.civilization.data.dao.*
import com.falcor.civilization.data.entities.*
import com.falcor.civilization.engine.ledger.LedgerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExportService(
    private val runDao: RunDao,
    private val runArtifactDao: RunArtifactDao,
    private val analysisResultDao: AnalysisResultDao,
    private val experimentConfigDao: ExperimentConfigDao,
    private val preregPlanDao: PreregPlanDao,
    private val ledgerService: LedgerService,
    private val reportGenerator: ReportGenerator,
    private val filesDir: File
) {
    private val json = Json { prettyPrint = true }

    suspend fun exportReproBundle(runId: String, projectId: String, outputFile: File) = withContext(Dispatchers.IO) {
        val run = runDao.getById(runId) ?: return@withContext
        val configId = run.configId
        val config = configId?.let { experimentConfigDao.getById(it) }
        val preregPlan = config?.preregPlanId?.let { preregPlanDao.getById(it) }
        val artifacts = runArtifactDao.getByRun(runId)
        val analysis = analysisResultDao.getByRun(runId)

        val runDir = File(filesDir, "projects/$projectId/runs/$runId")
        runDir.mkdirs()
        val configFile = File(runDir, "config.json")
        val artifactsFile = File(runDir, "artifacts.json")
        val rawFile = File(runDir, "raw.csv")
        val analysisFile = File(runDir, "analysis.json")
        val reportFile = File(runDir, "report.md")

        config?.let { c ->
            configFile.writeText(json.encodeToString(ConfigExport(
                id = c.id, preregPlanId = c.preregPlanId, seed = c.seed,
                simScenario = c.simScenario, sensorSet = c.sensorSet,
                controlParamsJson = c.controlParamsJson, createdAt = c.createdAt
            )))
        }
        artifactsFile.writeText(json.encodeToString(artifacts.map { a ->
            ArtifactExport(a.id, a.runId, a.artifactType, a.paramsJson, a.injectedByAgent, a.createdAt)
        }))
        analysis?.let { a ->
            analysisFile.writeText(json.encodeToString(AnalysisExport(
                a.id, a.runId, a.metricsJson, a.pValuesJson, a.posteriorJson,
                a.correctionJson, a.decision, a.createdAt
            )))
        }

        val configStr = config?.let { c ->
            json.encodeToString(ConfigExport(
                id = c.id, preregPlanId = c.preregPlanId, seed = c.seed,
                simScenario = c.simScenario, sensorSet = c.sensorSet,
                controlParamsJson = c.controlParamsJson, createdAt = c.createdAt
            ))
        }
        val reportContent = reportGenerator.generateRunReport(
            run = run,
            config = configStr,
            artifacts = artifacts,
            analysis = analysis,
            preregHash = preregPlan?.planHash
        )
        reportGenerator.writeReportToFile(reportContent, reportFile)

        val ledgerSlice = ledgerService.getSliceForRun(run.startedAt, run.endedAt ?: run.startedAt)

        outputFile.outputStream().use { outStream ->
            ZipOutputStream(outStream).use { zos ->
                fun addFile(path: String, file: File) {
                    if (file.exists()) {
                        zos.putNextEntry(ZipEntry(path))
                        file.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
                addFile("config.json", configFile)
                addFile("artifacts.json", artifactsFile)
                addFile("raw.csv", rawFile)
                addFile("analysis.json", analysisFile)
                addFile("report.md", reportFile)
                zos.putNextEntry(ZipEntry("ledger_slice.json"))
                zos.write(json.encodeToString(ledgerSlice.map { mapOf(
                    "id" to it.id,
                    "prevHash" to it.prevHash,
                    "payloadHash" to it.payloadHash,
                    "entryHash" to it.entryHash,
                    "createdAt" to it.createdAt
                ) }).toByteArray())
                zos.closeEntry()
            }
        }
    }
}
