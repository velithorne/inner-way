package com.falcor.civilization.domain

import kotlinx.serialization.Serializable

@Serializable
data class SimScenario(
    val id: String,
    val name: String,
    val description: String,
    val params: Map<String, Double>
)

@Serializable
data class SensorSet(
    val channels: List<String>,
    val sampleRateHz: Double,
    val resolution: Int
)

@Serializable
data class PreregPlanContent(
    val metrics: List<String>,
    val thresholds: Map<String, Double>,
    val stoppingRule: String,
    val exclusionCriteria: List<String>,
    val alpha: Double = 0.05,
    val minSampleSize: Int = 30
)

@Serializable
data class ArtifactParams(
    val type: String,
    val amplitude: Double,
    val frequency: Double? = null,
    val phase: Double? = null,
    val duration: Double? = null,
    val channel: String? = null
)

@Serializable
data class ObservationPoint(
    val t: Double,
    val channels: Map<String, Double>
)

@Serializable
data class MetricsResult(
    val metricName: String,
    val value: Double,
    val rawPValue: Double? = null,
    val effectSize: Double? = null
)

@Serializable
data class AnalysisMetrics(
    val metrics: List<MetricsResult>,
    val correctedPValues: Map<String, Double>? = null,
    val posteriorOdds: Double? = null,
    val decision: String,
    val integrityPassed: Boolean
)

@Serializable
data class LedgerPayload(
    val type: String,
    val entityId: String? = null,
    val data: String,
    val timestamp: Long
)
