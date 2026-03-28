package com.velithorne.vessel.util

import android.util.Log
import com.velithorne.vessel.core.Constants

object Logger {
    fun d(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.d(Constants.LOG_TAG, message, throwable)
        } else {
            Log.d(Constants.LOG_TAG, message)
        }
    }
}
