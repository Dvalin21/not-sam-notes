package com.openlight.notes

import android.app.Application
import android.util.Log

class NotesApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
        container = AppContainer(this)
        Log.i("NotSamNotes", "Application started")
    }
}
