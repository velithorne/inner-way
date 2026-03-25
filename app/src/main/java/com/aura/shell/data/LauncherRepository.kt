package com.aura.shell.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import com.aura.shell.LaunchActivityProvider
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

    /**
     * @return false only if no launch intent could be built — throws from startActivity are swallowed but rare with Activity context.
     */
    fun launchApp(packageName: String): Boolean {
        val intent = resolveLaunchIntent(packageName) ?: return false
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED,
        )
        val mainComponent = intent.component ?: resolveMainActivityComponent(packageName)
        val runnable = {
            val viaLauncher =
                mainComponent != null && tryStartViaLauncherApps(mainComponent)
            if (!viaLauncher) {
                val launchContext = LaunchActivityProvider.current() ?: appContext
                try {
                    launchContext.startActivity(intent)
                } catch (_: Exception) {
                    try {
                        appContext.startActivity(intent)
                    } catch (_: Exception) { }
                }
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable()
        } else {
            Handler(Looper.getMainLooper()).post(runnable)
        }
        return true
    }

    private fun resolveMainActivityComponent(packageName: String): ComponentName? {
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolves = queryLaunchActivities(main)
        val resolve = resolves.firstOrNull { it.activityInfo.packageName == packageName } ?: return null
        return ComponentName(
            resolve.activityInfo.packageName,
            resolve.activityInfo.name,
        )
    }

    /**
     * When Aura is the default HOME app, plain [Context.startActivity] from the launcher task can be
     * ignored on some devices. [LauncherApps.startMainActivity] is the supported path for launchers.
     */
    private fun tryStartViaLauncherApps(component: ComponentName): Boolean {
        return try {
            val launcherApps =
                appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
                    ?: return false
            launcherApps.startMainActivity(component, Process.myUserHandle(), null, null)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * [getLaunchIntentForPackage] is null for some apps/work profiles; fall back to explicit MAIN/LAUNCHER activity.
     */
    fun resolveLaunchIntent(packageName: String): Intent? {
        packageManager.getLaunchIntentForPackage(packageName)?.let {
            return it
        }
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolves = queryLaunchActivities(main)
        val resolve = resolves.firstOrNull { it.activityInfo.packageName == packageName } ?: return null
        return Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(
                resolve.activityInfo.packageName,
                resolve.activityInfo.name,
            )
        }
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
