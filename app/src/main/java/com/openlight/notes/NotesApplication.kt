package com.openlight.notes

import android.app.Application
import android.util.Log

/**
 * Application entry point.
 * Phase 0: minimal — DI container will be wired in Phase 1.
 */
class NotesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i("NotSamNotes", "Application started")
    }
}
