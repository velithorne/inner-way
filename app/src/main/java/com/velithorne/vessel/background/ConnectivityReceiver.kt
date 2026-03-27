package com.velithorne.vessel.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import com.velithorne.vessel.VesselApplication

/**
 * Coarse connectivity changes — records one passive snapshot (no polling loop).
 */
class ConnectivityReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        @Suppress("DEPRECATION")
        val connectivityAction = ConnectivityManager.CONNECTIVITY_ACTION
        if (intent?.action != connectivityAction &&
            intent?.action != "android.net.conn.CONNECTIVITY_CHANGE"
        ) return
        val app = context.applicationContext as? VesselApplication ?: return
        app.appContainer.recordConnectivityAmbientEvent()
    }
}
