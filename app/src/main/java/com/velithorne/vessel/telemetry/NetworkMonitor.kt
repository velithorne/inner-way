package com.velithorne.vessel.telemetry

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.content.ContextCompat

class NetworkMonitor(
    private val appContext: Context,
) {
    private val connectivity: ConnectivityManager? =
        ContextCompat.getSystemService(appContext, ConnectivityManager::class.java)

    fun read(): NetworkReading {
        val cm = connectivity ?: return NetworkReading(
            connected = null,
            transport = NetworkTransport.NONE,
            metered = null,
        )

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = cm.activeNetwork ?: return NetworkReading(
                    connected = false,
                    transport = NetworkTransport.NONE,
                    metered = null,
                )
                val caps = cm.getNetworkCapabilities(network) ?: return NetworkReading(
                    connected = false,
                    transport = NetworkTransport.NONE,
                    metered = null,
                )
                val connected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val transport = classifyTransport(caps)
                val metered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                NetworkReading(
                    connected = connected,
                    transport = transport,
                    metered = metered,
                )
            } else {
                readLegacyNetwork(cm)
            }
        } catch (_: Throwable) {
            NetworkReading(connected = null, transport = NetworkTransport.OTHER, metered = null)
        }
    }

    @Suppress("DEPRECATION")
    private fun readLegacyNetwork(cm: ConnectivityManager): NetworkReading {
        val info = cm.activeNetworkInfo
        val connected = info?.isConnected == true
        val type = info?.type
        val transport = when (type) {
            ConnectivityManager.TYPE_WIFI -> NetworkTransport.WIFI
            ConnectivityManager.TYPE_MOBILE,
            ConnectivityManager.TYPE_MOBILE_MMS,
            ConnectivityManager.TYPE_MOBILE_SUPL,
            ConnectivityManager.TYPE_MOBILE_DUN,
            ConnectivityManager.TYPE_MOBILE_HIPRI,
                -> NetworkTransport.CELLULAR

            ConnectivityManager.TYPE_ETHERNET -> NetworkTransport.ETHERNET
            ConnectivityManager.TYPE_VPN -> NetworkTransport.VPN
            ConnectivityManager.TYPE_BLUETOOTH -> NetworkTransport.BLUETOOTH
            null -> if (connected) NetworkTransport.OTHER else NetworkTransport.NONE
            else -> NetworkTransport.OTHER
        }
        return NetworkReading(
            connected = connected,
            transport = transport,
            metered = null,
        )
    }

    private fun classifyTransport(caps: NetworkCapabilities): NetworkTransport {
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkTransport.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkTransport.CELLULAR
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkTransport.ETHERNET
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkTransport.VPN
            caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> NetworkTransport.BLUETOOTH
            else -> NetworkTransport.OTHER
        }
    }

    data class NetworkReading(
        val connected: Boolean?,
        val transport: NetworkTransport,
        val metered: Boolean?,
    )
}
