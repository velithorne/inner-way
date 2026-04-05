package com.velithorne.innerway.genome

import android.content.Context
import androidx.core.content.edit
import org.json.JSONObject

/**
 * Mutable living tendencies; persisted as JSON in SharedPreferences.
 */
data class ActiveGenome(
    val heatSensitivity: Float = 1f,
    val energyConservation: Float = 0.5f,
    val curiosity: Float = 0.35f,
    val sleepDepth: Float = 0.55f,
    val responsiveness: Float = 0.6f,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("heatSensitivity", heatSensitivity.toDouble())
        put("energyConservation", energyConservation.toDouble())
        put("curiosity", curiosity.toDouble())
        put("sleepDepth", sleepDepth.toDouble())
        put("responsiveness", responsiveness.toDouble())
    }

    companion object {
        private const val PREFS = "velithorne_genome"
        private const val KEY_ACTIVE = "active_genome_v1"

        fun load(context: Context): ActiveGenome {
            val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_ACTIVE, null) ?: return ActiveGenome()
            return runCatching {
                val o = JSONObject(raw)
                ActiveGenome(
                    heatSensitivity = o.optDouble("heatSensitivity", 1.0).toFloat(),
                    energyConservation = o.optDouble("energyConservation", 0.5).toFloat(),
                    curiosity = o.optDouble("curiosity", 0.35).toFloat(),
                    sleepDepth = o.optDouble("sleepDepth", 0.55).toFloat(),
                    responsiveness = o.optDouble("responsiveness", 0.6).toFloat(),
                )
            }.getOrDefault(ActiveGenome())
        }

        fun persist(context: Context, genome: ActiveGenome) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
                putString(KEY_ACTIVE, genome.toJson().toString())
            }
        }

        fun persistDefault(context: Context) {
            persist(context, ActiveGenome())
        }
    }
}
