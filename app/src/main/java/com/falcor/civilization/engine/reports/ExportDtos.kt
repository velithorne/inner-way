package com.falcor.civilization.engine.reports

import kotlinx.serialization.Serializable

@Serializable
data class ConfigExport(
    val id: String,
    val preregPlanId: String,
    val seed: Long,
    val simScenario: String,
    val sensorSet: String,
    val controlParamsJson: String,
    val createdAt: Long
)

@Serializable
data class ArtifactExport(
    val id: String,
    val runId: String,
    val artifactType: String,
    val paramsJson: String,
    val injectedByAgent: String,
    val createdAt: Long
)

@Serializable
data class AnalysisExport(
    val id: String,
    val runId: String,
    val metricsJson: String,
    val pValuesJson: String,
    val posteriorJson: String,
    val correctionJson: String,
    val decision: String,
    val createdAt: Long
)
