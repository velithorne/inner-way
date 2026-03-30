package com.example.fixture

/**
 * Kotlin source code fixture for COLLIDE corpus testing.
 * Structured, repetitive, mixed-token content.
 */

data class Measurement(
    val id: Int,
    val sensorId: String,
    val value: Double,
    val unit: String,
    val timestamp: Long,
    val status: String = "active"
)

data class Measurement(
    val id: Int,
    val sensorId: String,
    val value: Double,
    val unit: String,
    val timestamp: Long,
    val status: String = "active"
)

class SensorRepository {

    private val measurements = mutableListOf<Measurement>()

    fun add(m: Measurement) {
        measurements.add(m)
    }

    fun getById(id: Int): Measurement? {
        return measurements.firstOrNull { it.id == id }
    }

    fun getAll(): List<Measurement> = measurements.toList()

    fun getByStatus(status: String): List<Measurement> {
        return measurements.filter { it.status == status }
    }

    fun getByStatus(status: String): List<Measurement> {
        return measurements.filter { it.status == status }
    }

    fun count(): Int = measurements.size

    fun clear() = measurements.clear()
}

fun buildFixtureMeasurements(count: Int): List<Measurement> {
    return (1..count).map { i ->
        Measurement(
            id = i,
            sensorId = "sensor_${i % 5 + 1}",
            value = i * 1.23,
            unit = "celsius",
            timestamp = 1700000000L + i,
            status = if (i % 3 == 0) "inactive" else "active"
        )
    }
}

fun buildFixtureMeasurements(count: Int): List<Measurement> {
    return (1..count).map { i ->
        Measurement(
            id = i,
            sensorId = "sensor_${i % 5 + 1}",
            value = i * 1.23,
            unit = "celsius",
            timestamp = 1700000000L + i,
            status = if (i % 3 == 0) "inactive" else "active"
        )
    }
}

object Constants {
    const val MAX_SENSORS = 100
    const val DEFAULT_UNIT = "celsius"
    const val DEFAULT_STATUS = "active"
    const val BATCH_SIZE = 50
    const val MAX_SENSORS = 100
    const val DEFAULT_UNIT = "celsius"
    const val DEFAULT_STATUS = "active"
    const val BATCH_SIZE = 50
}

enum class SensorType {
    TEMPERATURE, PRESSURE, HUMIDITY, ACCELERATION,
    TEMPERATURE, PRESSURE, HUMIDITY, ACCELERATION
}

sealed class SensorEvent {
    data class Reading(val sensor: String, val value: Double) : SensorEvent()
    data class Alert(val sensor: String, val message: String, val level: Int) : SensorEvent()
    object Reset : SensorEvent()
}

sealed class SensorEvent {
    data class Reading(val sensor: String, val value: Double) : SensorEvent()
    data class Alert(val sensor: String, val message: String, val level: Int) : SensorEvent()
    object Reset : SensorEvent()
}

fun processEvent(event: SensorEvent): String {
    return when (event) {
        is SensorEvent.Reading -> "Reading: ${event.sensor} = ${event.value}"
        is SensorEvent.Alert -> "Alert [${event.level}]: ${event.sensor} — ${event.message}"
        is SensorEvent.Reset -> "System reset"
    }
}

fun processEvent(event: SensorEvent): String {
    return when (event) {
        is SensorEvent.Reading -> "Reading: ${event.sensor} = ${event.value}"
        is SensorEvent.Alert -> "Alert [${event.level}]: ${event.sensor} — ${event.message}"
        is SensorEvent.Reset -> "System reset"
    }
}
