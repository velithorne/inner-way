package com.velithorne.vessel.background

/**
 * How an ecology row was captured — drives analytics and return summaries.
 */
enum class EcologySnapshotSourceType {
    PERIODIC_WORK,
    APP_RESUME,
    CHARGING_EVENT,
    CONNECTIVITY_EVENT,
    APP_BACKGROUND,
    APP_FOREGROUND,
    MANUAL_DEBUG,
}
