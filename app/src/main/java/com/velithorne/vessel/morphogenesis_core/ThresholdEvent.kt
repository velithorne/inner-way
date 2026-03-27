package com.velithorne.vessel.morphogenesis_core

enum class ThresholdEventKind {
    FIRST_HEAT_CRISIS,
    FIRST_CLEAN_CHARGING_WEEK,
    FIRST_FROND_EXTENSION,
    FIRST_SCAR,
    FIRST_MOLT,
    FIRST_ARCHIVE_PLATE,
}

data class ThresholdEvent(
    val kind: ThresholdEventKind,
    val timestampMillis: Long,
)
