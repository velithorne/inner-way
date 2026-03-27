package com.velithorne.vessel.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.velithorne.vessel.VesselApplication

/**
 * Lightweight power connection events — no wake lock; WorkManager still handles coarse periodic sampling.
 */
class ChargingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_POWER_CONNECTED && intent?.action != Intent.ACTION_POWER_DISCONNECTED) return
        val app = context.applicationContext as? VesselApplication ?: return
        val connected = intent.action == Intent.ACTION_POWER_CONNECTED
        app.appContainer.recordChargingAmbientEvent(connected)
    }
}
