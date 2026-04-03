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
import android.os.storage.StorageManager
import android.os.storage.StorageStatsManager
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class SystemDataModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  private var lastRxTotal: Long = -1L
  private var lastTxTotal: Long = -1L
  private var lastNetAtMs: Long = 0L

  private var prevCpuIdle: LongArray? = null
  private var prevCpuTotal: LongArray? = null

  override fun getName(): String = "SystemData"

  @ReactMethod
  fun getCpuCoreUsage(promise: Promise) {
    try {
      val lines = readProcStatLines()
      if (lines.isEmpty()) {
        promise.resolve(Arguments.createArray())
        return
      }

      val perCpuLines = lines.filter { it.matches(Regex("^cpu\\d+\\s.*")) }
      val n = perCpuLines.size
      if (n == 0) {
        promise.resolve(Arguments.createArray())
        return
      }

      val idle = LongArray(n)
      val total = LongArray(n)
      for (i in 0 until n) {
        val parts = perCpuLines[i].trim().split(Regex("\\s+"))
        if (parts.size < 5) continue
        var sum = 0L
        for (j in 1 until parts.size) {
          sum += parts[j].toLong()
        }
        total[i] = sum
        idle[i] = parts[4].toLong()
      }

      val prevIdleArr = prevCpuIdle
      val prevTotalArr = prevCpuTotal
      prevCpuIdle = idle.copyOf()
      prevCpuTotal = total.copyOf()

      val out = Arguments.createArray()
      if (prevIdleArr == null || prevTotalArr == null ||
        prevIdleArr.size != n || prevTotalArr.size != n
      ) {
        for (i in 0 until n) {
          val m = Arguments.createMap()
          m.putInt("core", i)
          m.putInt("usage", 0)
          out.pushMap(m)
        }
        promise.resolve(out)
        return
      }

      for (i in 0 until n) {
        val dIdle = idle[i] - prevIdleArr[i]
        val dTotal = total[i] - prevTotalArr[i]
        val usage = if (dTotal > 0) {
          (((dTotal - dIdle) * 100.0 / dTotal).coerceIn(0.0, 100.0)).toInt()
        } else {
          0
        }
        val m = Arguments.createMap()
        m.putInt("core", i)
        m.putInt("usage", usage)
        out.pushMap(m)
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
      val processes = am.runningAppProcesses ?: run {
        promise.resolve(Arguments.createArray())
        return
      }

      val pids = IntArray(processes.size)
      for (i in processes.indices) {
        pids[i] = processes[i].pid
      }
      val memInfos = am.getProcessMemoryInfo(pids)

      val out = Arguments.createArray()
      for (i in processes.indices) {
        val proc = processes[i]
        val pkg = proc.processName ?: continue
        val mi = memInfos.getOrNull(i) ?: continue
        val pssKb = mi.totalPss.toLong()
        val memoryBytes = pssKb * 1024L

        val label = try {
          val appInfo = pm.getApplicationInfo(pkg, 0)
          pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
          pkg
        }

        val importance = when (proc.importance) {
          ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> "FOREGROUND"
          ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE -> "VISIBLE"
          ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE -> "SERVICE"
          ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED -> "CACHED"
          else -> "OTHER_${proc.importance}"
        }

        val row = Arguments.createMap()
        row.putString("packageName", pkg)
        row.putString("appName", label)
        row.putDouble("memoryBytes", memoryBytes.toDouble())
        row.putInt("pid", proc.pid)
        row.putString("importance", importance)
        out.pushMap(row)
      }
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
      val dataStat = StatFs(Environment.getDataDirectory().absolutePath)
      val totalBytes = dataStat.blockCountLong * dataStat.blockSizeLong
      val freeBytes = dataStat.availableBytesLong
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

      var mediaBytes = 0L
      try {
        val ext = Environment.getExternalStorageDirectory()
        if (ext != null && ext.exists()) {
          mediaBytes = folderSize(ext)
        }
      } catch (_: Exception) {
        mediaBytes = 0L
      }

      val m = Arguments.createMap()
      m.putDouble("totalBytes", totalBytes.toDouble())
      m.putDouble("usedBytes", usedBytes.toDouble())
      m.putDouble("freeBytes", freeBytes.toDouble())
      m.putDouble("appDataBytes", appDataBytes.toDouble())
      m.putDouble("mediaBytes", mediaBytes.toDouble())
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

      val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
      val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
      val pct = if (level >= 0 && scale > 0) ((level * 100) / scale) else -1

      val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
      val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL

      val voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
      val tempTenth = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)

      val bm = reactApplicationContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
      val currentUa = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
      } else {
        -1
      }

      val m = Arguments.createMap()
      m.putInt("level", pct.coerceIn(0, 100))
      m.putBoolean("isCharging", isCharging)
      m.putInt("voltage", voltage)
      m.putDouble("temperature", if (tempTenth >= 0) tempTenth / 10.0 else -1.0)
      m.putInt("currentNow", currentUa)
      promise.resolve(m)
    } catch (e: Exception) {
      promise.reject("E_BATTERY", e.message, e)
    }
  }

  private fun readProcStatLines(): List<String> {
    val out = ArrayList<String>()
    BufferedReader(FileReader("/proc/stat")).use { br ->
      while (true) {
        val line = br.readLine() ?: break
        if (line.startsWith("cpu")) {
          out.add(line)
        }
      }
    }
    return out
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
