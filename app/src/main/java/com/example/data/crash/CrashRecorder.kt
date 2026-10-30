package com.example.data.crash

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

// Temporary debug code: lightweight crash recorder to capture unhandled exceptions
object CrashRecorder {
    private const val CRASH_FILE_NAME = "last_crash.txt"

    fun init(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                val stackTrace = sw.toString()

                val file = File(context.filesDir, CRASH_FILE_NAME)
                file.writeText(stackTrace)
            } catch (_: Throwable) {
                // Ignore failure in crash handler
            }

            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(10)
            }
        }
    }

    fun getSavedCrash(context: Context): String? {
        val file = File(context.filesDir, CRASH_FILE_NAME)
        if (!file.exists()) return null
        return try {
            val text = file.readText()
            text.ifBlank { null }
        } catch (_: Throwable) {
            null
        }
    }

    fun clearSavedCrash(context: Context) {
        try {
            val file = File(context.filesDir, CRASH_FILE_NAME)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Throwable) {
            // Ignore
        }
    }
}
