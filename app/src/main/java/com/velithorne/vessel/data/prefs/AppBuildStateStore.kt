package com.velithorne.vessel.data.prefs

import android.content.Context

private const val PREFS = "velithorne_build"
private const val KEY_VERSION_CODE = "last_version_code"

class AppBuildStateStore(context: Context) {
    private val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getLastVersionCode(): Int = p.getInt(KEY_VERSION_CODE, -1)

    fun setLastVersionCode(code: Int) {
        p.edit().putInt(KEY_VERSION_CODE, code).apply()
    }
}
