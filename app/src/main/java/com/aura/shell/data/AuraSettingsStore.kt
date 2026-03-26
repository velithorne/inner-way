package com.aura.shell.data

import android.content.Context
import androidx.core.content.edit

class AuraSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var passiveHandsFreeEnabled: Boolean
        get() = prefs.getBoolean(KEY_PASSIVE, false)
        set(value) {
            prefs.edit { putBoolean(KEY_PASSIVE, value) }
        }

    companion object {
        private const val PREFS = "aura_settings"
        private const val KEY_PASSIVE = "passive_hands_free"
    }
}
