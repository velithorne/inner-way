package com.velithorne.innerway.body

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Network atmosphere: connectivity as respiration openness.
 */
class SignalRespirationSystem(
    private val context: Context,
) {

    data class Sample(
        val atmosphereOpenness: Float,
        val timestampMillis: Long,
    )

    suspend fun sample(): Sample {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }
        val openness = when {
            caps == null -> 0.1f
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> 0.85f
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> 0.65f
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> 0.9f
            else -> 0.4f
        }
        return Sample(atmosphereOpenness = openness, timestampMillis = System.currentTimeMillis())
    }
}
