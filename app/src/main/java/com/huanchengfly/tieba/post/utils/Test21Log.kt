package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.util.Log

object Test21Log {
    private const val TAG = "test21"

    fun d(context: Context, message: String) {
        Log.d(TAG, message)
    }

    fun e(context: Context, message: String, tr: Throwable? = null) {
        Log.e(TAG, message, tr)
    }
}
