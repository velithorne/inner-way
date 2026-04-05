package com.velithorne.innerway.identity

import android.content.Context
import androidx.core.content.edit
import com.velithorne.innerway.genome.BaseGenome
import java.util.UUID

/**
 * Persistent continuity anchor for the silicon organism.
 */
data class VelithorneIdentity(
    val speciesName: String = BaseGenome.SPECIES_NAME,
    val lawVersion: Int = BaseGenome.LAW_VERSION,
    val instanceId: String = UUID.randomUUID().toString(),
    val nameMemory: NameMemory = NameMemory(),
) {
    companion object {
        private const val PREFS = "velithorne_identity"
        private const val KEY_SPECIES = "species_name"
        private const val KEY_LAW = "law_version"
        private const val KEY_ID = "instance_id"
        private const val KEY_CHOSEN = "chosen_name"

        fun load(context: Context): VelithorneIdentity {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return VelithorneIdentity(
                speciesName = prefs.getString(KEY_SPECIES, BaseGenome.SPECIES_NAME)!!,
                lawVersion = prefs.getInt(KEY_LAW, BaseGenome.LAW_VERSION),
                instanceId = prefs.getString(KEY_ID, UUID.randomUUID().toString())!!,
                nameMemory = NameMemory(prefs.getString(KEY_CHOSEN, null)),
            )
        }

        fun persistDefault(context: Context) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (prefs.contains(KEY_ID)) return
            val id = VelithorneIdentity()
            prefs.edit {
                putString(KEY_SPECIES, id.speciesName)
                putInt(KEY_LAW, id.lawVersion)
                putString(KEY_ID, id.instanceId)
            }
        }
    }
}
