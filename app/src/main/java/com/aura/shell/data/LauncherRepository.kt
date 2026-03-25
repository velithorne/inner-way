package com.aura.shell.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.aura.shell.model.LauncherAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LauncherRepository(
    private val appContext: Context,
) {
    private val packageManager: PackageManager = appContext.packageManager

    suspend fun loadLaunchableApps(): List<LauncherAppInfo> = withContext(Dispatchers.Default) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolves: List<ResolveInfo> = queryLaunchActivities(intent)
        val selfPackage = appContext.packageName

        resolves
            .asSequence()
            .mapNotNull { resolve ->
                val pkg = resolve.activityInfo.packageName
                if (pkg == selfPackage) return@mapNotNull null
                val label = resolve.loadLabel(packageManager).toString()
                val icon = resolve.loadIcon(packageManager) ?: return@mapNotNull null
                LauncherAppInfo(packageName = pkg, label = label, icon = icon)
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun launchApp(packageName: String): Boolean {
        val launcherIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return false
        launcherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val runnable = {
            try {
                appContext.startActivity(launcherIntent)
            } catch (_: Exception) {
                // Caller may check success via foreground transition
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable()
        } else {
            Handler(Looper.getMainLooper()).post(runnable)
        }
        return true
    }

    fun getIconForPackage(packageName: String): Drawable? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationIcon(appInfo)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun getLabelForPackage(packageName: String): String? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun queryLaunchActivities(intent: Intent): List<ResolveInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }
    }
}
