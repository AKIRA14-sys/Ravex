package com.akira.ravex.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RavexGuard {

    private const val TAG = "RavexGuard"
    private const val LOG_FILE_NAME = "ravex_crash_logs.txt"

    fun init(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logCrash(context, thread, throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun logCrash(context: Context, thread: Thread, throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTraceStr = sw.toString()

        val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val logEntry = """
            ========================================
            TIMESTAMP: $timeStamp
            THREAD: ${thread.name} [ID: ${thread.id}]
            EXCEPTION: ${throwable.javaClass.name}: ${throwable.message}
            STACKTRACE:
            $stackTraceStr
            ========================================

        """.trimIndent()

        Log.e(TAG, logEntry)

        try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            file.appendText(logEntry)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCrashLogs(context: Context): String {
        return try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists()) file.readText() else "No crash diagnostics logged."
        } catch (e: Exception) {
            "Failed to read crash logs: ${e.message}"
        }
    }

    fun clearCrashLogs(context: Context) {
        try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
