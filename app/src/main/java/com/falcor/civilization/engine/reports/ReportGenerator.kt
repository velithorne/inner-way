package com.falcor.civilization.engine.reports

import com.falcor.civilization.data.entities.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class ReportGenerator(private val json: Json = Json { prettyPrint = true }) {

    fun generateRunReport(
        run: RunEntity,
        config: String? = null,
        artifacts: List<RunArtifactEntity>,
        analysis: AnalysisResultEntity?,
        preregHash: String?
    ): String {
        val sb = StringBuilder()
        sb.appendLine("# Run Report: ${run.id}")
        sb.appendLine()
        sb.appendLine("## Methods")
        sb.appendLine("- **Run ID**: ${run.id}")
        sb.appendLine("- **Mode**: ${run.mode}")
        sb.appendLine("- **Blind**: ${run.blind}")
        sb.appendLine("- **Config**: ${config ?: "N/A"}")
        sb.appendLine("- **Prereg Hash**: ${preregHash ?: "N/A"}")
        sb.appendLine()
        sb.appendLine("## Injected Artifacts")
        if (artifacts.isEmpty()) {
            sb.appendLine("None")
        } else {
            artifacts.forEach { a ->
                sb.appendLine("- ${a.artifactType} (by ${a.injectedByAgent})")
            }
        }
        sb.appendLine()
        sb.appendLine("## Results")
        if (analysis != null) {
            sb.appendLine("- **Decision**: ${analysis.decision}")
            sb.appendLine("- **Metrics**: ${analysis.metricsJson}")
            sb.appendLine("- **Corrected p-values**: ${analysis.correctionJson}")
        } else {
            sb.appendLine("No analysis available")
        }
        sb.appendLine()
        sb.appendLine("## Repro Steps")
        sb.appendLine("Replay run by ID: `${run.id}` with same config and seed.")
        return sb.toString()
    }

    fun generateFindingReport(
        finding: FindingEntity,
        hypothesis: HypothesisEntity?
    ): String {
        val sb = StringBuilder()
        sb.appendLine("# Finding Report: ${finding.id}")
        sb.appendLine()
        sb.appendLine("## Summary")
        sb.appendLine(finding.summary)
        sb.appendLine()
        sb.appendLine("## Evidence")
        sb.appendLine("- **Evidence Score**: ${finding.evidenceScore}")
        sb.appendLine("- **Replication Score**: ${finding.replicationScore}")
        sb.appendLine()
        sb.appendLine("## Hypothesis")
        sb.appendLine(hypothesis?.title ?: "Unknown")
        sb.appendLine(hypothesis?.description ?: "")
        return sb.toString()
    }

    fun writeReportToFile(content: String, file: File) {
        file.parentFile?.mkdirs()
        file.writeText(content)
    }
}
