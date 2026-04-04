package com.phantom.app

import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class WifiScanModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  override fun getName(): String = "WifiScanModule"

  private val wifiManager: WifiManager
    get() =
      reactApplicationContext.applicationContext
        .getSystemService(android.content.Context.WIFI_SERVICE) as WifiManager

  @ReactMethod
  fun scanNetworks(promise: Promise) {
    try {
      @Suppress("DEPRECATION")
      val started = wifiManager.startScan()
      val delayMs = if (started) 450L else 800L
      Handler(Looper.getMainLooper()).postDelayed({
        try {
          @Suppress("MissingPermission")
          val results = wifiManager.scanResults ?: emptyList()
          val now = System.currentTimeMillis().toDouble()
          val array = Arguments.createArray()
          for (result in results) {
            val map = Arguments.createMap()
            map.putString("ssid", result.SSID ?: "")
            map.putString("bssid", result.BSSID ?: "")
            map.putInt("rssi", result.level)
            map.putInt("frequency", result.frequency)
            map.putInt("channel", channelFromFrequency(result.frequency))
            map.putString("capabilities", result.capabilities ?: "")
            map.putDouble("timestamp", now)
            array.pushMap(map)
          }
          promise.resolve(array)
        } catch (e: Exception) {
          promise.reject("SCAN_ERROR", e.message, e)
        }
      }, delayMs)
    } catch (e: Exception) {
      promise.reject("SCAN_ERROR", e.message, e)
    }
  }

  private fun channelFromFrequency(freq: Int): Int {
    return when {
      freq in 2412..2484 -> (freq - 2412) / 5 + 1
      freq >= 5180 -> (freq - 5180) / 5 + 36
      else -> 0
    }
  }
}
