package com.silicon.app

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.Process
import android.os.StatFs
import android.app.usage.StorageStatsManager
import android.os.storage.StorageManager
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import java.io.File
import kotlin.math.abs

/**
 * System telemetry without /proc/stat (blocked on Android 8+ for apps).
 * CPU: per-core cpufreq ratio (cur/max) from sysfs — load proxy, no permission.
 * Apps: own process + "Other processes" aggregate (getRunningAppProcesses is restricted on 11+).
 */
class SystemDataModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  private var lastRxTotal: Long = -1L
  private var lastTxTotal: Long = -1L
  private var lastNetAtMs: Long = 0L

  override fun getName(): String = "SystemData"

  @ReactMethod
  fun getCpuCoreUsage(promise: Promise) {
    try {
      val out = Arguments.createArray()
      var core = 0
      while (true) {
        val curFile = File("/sys/devices/system/cpu/cpu$core/cpufreq/scaling_cur_freq")
        val maxFile = File("/sys/devices/system/cpu/cpu$core/cpufreq/cpuinfo_max_freq")
        if (!curFile.exists()) break

        val cur = curFile.readText().trim().toLongOrNull() ?: 0L
        val max = if (maxFile.exists()) {
          maxFile.readText().trim().toLongOrNull() ?: 1L
        } else {
          1L
        }
        val maxSafe = max.coerceAtLeast(1L)
        val usage = ((cur.toFloat() / maxSafe.toFloat()) * 100f).toInt().coerceIn(0, 100)

        val m = Arguments.createMap()
        m.putInt("core", core)
        m.putInt("usage", usage)
        m.putDouble("curFreqKhz", cur.toDouble())
        m.putDouble("maxFreqKhz", max.toDouble())
        out.pushMap(m)
        core++
      }
      promise.resolve(out)
    } catch (e: Exception) {
      promise.reject("E_CPU", e.message, e)
    }
  }

  @ReactMethod
  fun getMemoryInfo(promise: Promise) {
    try {
      val am = reactApplicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
      val info = ActivityManager.MemoryInfo()
      am.getMemoryInfo(info)
      val m = Arguments.createMap()
      m.putDouble("totalRam", info.totalMem.toDouble())
      m.putDouble("availableRam", info.availMem.toDouble())
      m.putDouble("usedRam", (info.totalMem - info.availMem).toDouble())
      m.putBoolean("lowMemory", info.lowMemory)
      m.putDouble("threshold", info.threshold.toDouble())
      m.putInt("memoryClassMb", am.memoryClass)
      promise.resolve(m)
    } catch (e: Exception) {
      promise.reject("E_MEM", e.message, e)
    }
  }

  @ReactMethod
  fun getRunningApps(promise: Promise) {
    try {
      val am = reactApplicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
      val pm = reactApplicationContext.packageManager
      val ctx = reactApplicationContext

      val memInfo = ActivityManager.MemoryInfo()
      am.getMemoryInfo(memInfo)
      val usedRamBytes = memInfo.totalMem - memInfo.availMem

      val myPid = Process.myPid()
      val myPkg = ctx.packageName
      val pids = intArrayOf(myPid)
      val procMem = am.getProcessMemoryInfo(pids)
      val myMi = procMem.getOrNull(0)
      val myPssKb = myMi?.totalPss?.toLong() ?: 0L
      val myMemoryBytes = myPssKb * 1024L

      val myLabel = try {
        val ai = pm.getApplicationInfo(myPkg, 0)
        pm.getApplicationLabel(ai).toString()
      } catch (_: Exception) {
        "SILICON"
      }

      val out = Arguments.createArray()

      val selfRow = Arguments.createMap()
      selfRow.putString("packageName", myPkg)
      selfRow.putString("appName", myLabel)
      selfRow.putDouble("memoryBytes", myMemoryBytes.toDouble())
      selfRow.putInt("pid", myPid)
      selfRow.putString("importance", "SELF")
      out.pushMap(selfRow)

      val otherBytes = (usedRamBytes - myMemoryBytes).coerceAtLeast(0L)
      val otherRow = Arguments.createMap()
      otherRow.putString("packageName", "__silicon_aggregate_other__")
      otherRow.putString("appName", "Other processes")
      otherRow.putDouble("memoryBytes", otherBytes.toDouble())
      otherRow.putInt("pid", -1)
      otherRow.putString("importance", "AGGREGATE")
      out.pushMap(otherRow)

      promise.resolve(out)
    } catch (e: Exception) {
      promise.reject("E_APPS", e.message, e)
    }
  }

  @ReactMethod
  fun getNetworkTraffic(promise: Promise) {
    try {
      val now = System.currentTimeMillis()
      var rx = TrafficStats.getTotalRxBytes()
      var tx = TrafficStats.getTotalTxBytes()
      if (rx == TrafficStats.UNSUPPORTED.toLong()) rx = 0L
      if (tx == TrafficStats.UNSUPPORTED.toLong()) tx = 0L

      var rxPerSec = 0.0
      var txPerSec = 0.0
      if (lastRxTotal >= 0 && lastTxTotal >= 0 && lastNetAtMs > 0) {
        val dt = (now - lastNetAtMs) / 1000.0
        if (dt > 0) {
          rxPerSec = ((rx - lastRxTotal).coerceAtLeast(0)) / dt
          txPerSec = ((tx - lastTxTotal).coerceAtLeast(0)) / dt
        }
      }
      lastRxTotal = rx
      lastTxTotal = tx
      lastNetAtMs = now

      val m = Arguments.createMap()
      m.putDouble("rxBytesTotal", rx.toDouble())
      m.putDouble("txBytesTotal", tx.toDouble())
      m.putDouble("rxBytesPerSecond", rxPerSec)
      m.putDouble("txBytesPerSecond", txPerSec)
      promise.resolve(m)
    } catch (e: Exception) {
      promise.reject("E_NET", e.message, e)
    }
  }

  @ReactMethod
  fun getStorageInfo(promise: Promise) {
    try {
      val dataPath = Environment.getDataDirectory().absolutePath
      val dataStat = StatFs(dataPath)
      val totalBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        dataStat.totalBytes
      } else {
        dataStat.blockCountLong * dataStat.blockSizeLong
      }
      val freeBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        dataStat.freeBytes
      } else {
        dataStat.availableBlocksLong * dataStat.blockSizeLong
      }
      val usedBytes = totalBytes - freeBytes

      var appDataBytes = 0L
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        try {
          val sm = reactApplicationContext.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
          val stats = sm.queryStatsForUid(StorageManager.UUID_DEFAULT, Process.myUid())
          appDataBytes = stats.appBytes + stats.cacheBytes
        } catch (_: Exception) {
          appDataBytes = folderSize(File(reactApplicationContext.applicationInfo.dataDir))
        }
      } else {
        appDataBytes = folderSize(File(reactApplicationContext.applicationInfo.dataDir))
      }

      var extTotal = 0L
      var extFree = 0L
      try {
        val ext = Environment.getExternalStorageDirectory()
        if (ext != null && ext.exists()) {
          val extStat = StatFs(ext.absolutePath)
          extTotal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            extStat.totalBytes
          } else {
            extStat.blockCountLong * extStat.blockSizeLong
          }
          extFree = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            extStat.freeBytes
          } else {
            extStat.availableBlocksLong * extStat.blockSizeLong
          }
        }
      } catch (_: Exception) {
        extTotal = 0L
        extFree = 0L
      }

      val m = Arguments.createMap()
      m.putDouble("totalBytes", totalBytes.toDouble())
      m.putDouble("usedBytes", usedBytes.toDouble())
      m.putDouble("freeBytes", freeBytes.toDouble())
      m.putDouble("appDataBytes", appDataBytes.toDouble())
      m.putDouble("externalTotalBytes", extTotal.toDouble())
      m.putDouble("externalFreeBytes", extFree.toDouble())
      promise.resolve(m)
    } catch (e: Exception) {
      promise.reject("E_STORAGE", e.message, e)
    }
  }

  @ReactMethod
  fun getBatteryDetails(promise: Promise) {
    try {
      val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
      val batteryStatus = reactApplicationContext.registerReceiver(null, ifilter)
        ?: run {
          promise.reject("E_BATTERY", "No battery intent")
          return
        }

      val bm = reactApplicationContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

      var pct = -1
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val cap = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if (cap in 0..100) pct = cap
      }
      if (pct < 0) {
        val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        pct = if (level >= 0 && scale > 0) ((level * 100) / scale) else -1
      }

      val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
      val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL

      val voltageMv = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
      val tempTenth = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)

      val currentRaw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
      } else {
        Int.MIN_VALUE
      }
      val currentUa = if (currentRaw == Int.MIN_VALUE) -1 else currentRaw

      var powerWatts = -1.0
      if (voltageMv > 0 && currentUa != -1) {
        val amps = abs(currentRaw.toDouble()) / 1_000_000.0
        powerWatts = (voltageMv / 1000.0) * amps
      }

      val m = Arguments.createMap()
      m.putInt("level", pct.coerceIn(0, 100))
      m.putBoolean("isCharging", isCharging)
      m.putInt("voltage", voltageMv)
      m.putDouble("temperature", if (tempTenth >= 0) tempTenth / 10.0 else -1.0)
      m.putInt("currentNow", currentUa)
      m.putDouble("powerWatts", powerWatts)
      promise.resolve(m)
    } catch (e: Exception) {
      promise.reject("E_BATTERY", e.message, e)
    }
  }

  private fun folderSize(dir: File): Long {
    if (!dir.exists()) return 0L
    var size = 0L
    val files = dir.listFiles() ?: return 0L
    for (f in files) {
      size += if (f.isDirectory) folderSize(f) else f.length()
    }
    return size
  }
}
