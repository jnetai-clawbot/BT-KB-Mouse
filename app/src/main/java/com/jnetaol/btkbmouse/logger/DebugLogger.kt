package com.jnetaol.btkbmouse.logger

import android.util.Log

object DebugLogger {
    private const val TAG = "BTKBMousePro"
    var enableLogging = true

    fun d(tag: String, message: String) {
        if (enableLogging) Log.d(TAG, "[$tag] $message")
    }

    fun i(tag: String, message: String) {
        if (enableLogging) Log.i(TAG, "[$tag] $message")
    }

    fun w(tag: String, message: String) {
        Log.w(TAG, "[$tag] $message")
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(TAG, "[$tag] $message", throwable)
    }
}
