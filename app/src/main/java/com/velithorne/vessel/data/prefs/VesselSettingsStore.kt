package com.velithorne.vessel.data.prefs

import android.content.Context

/** Reserved for user preferences (notifications, theme hooks). */
class VesselSettingsStore(context: Context) {
    private val p = context.applicationContext.getSharedPreferences("velithorne_settings", Context.MODE_PRIVATE)

    fun getLineageOnboardingSeen(): Boolean = p.getBoolean("lineage_onboarding", false)

    fun setLineageOnboardingSeen() {
        p.edit().putBoolean("lineage_onboarding", true).apply()
    }
}
