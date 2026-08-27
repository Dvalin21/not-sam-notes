package com.openlight.notes

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Uncaught exception handler (Phase 10).
 * Writes crash logs to app files dir for debugging.
 */
class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val trace = sw.toString()

            val logFile = File(context.filesDir, "crash_${System.currentTimeMillis()}.txt")
            logFile.writeText(trace)

            Log.e("NotSamNotes", "Uncaught exception", throwable)
        } catch (e: Exception) {
            Log.e("NotSamNotes", "Failed to write crash log", e)
        } finally {
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        fun install(context: Context) {
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(context))
        }
    }
}
