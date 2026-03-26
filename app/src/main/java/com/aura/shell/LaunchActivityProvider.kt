package com.aura.shell

import android.app.Activity
import java.lang.ref.WeakReference

/**
 * Last resumed Activity for [com.aura.shell.data.LauncherRepository] — [startActivity] is more reliable
 * with an Activity context than [android.app.Application] on some OEM builds.
 */
object LaunchActivityProvider {

    private var ref: WeakReference<Activity>? = null

    fun attach(activity: Activity) {
        ref = WeakReference(activity)
    }

    fun detach(activity: Activity) {
        if (ref?.get() === activity) {
            ref = null
        }
    }

    fun current(): Activity? = ref?.get()
}
