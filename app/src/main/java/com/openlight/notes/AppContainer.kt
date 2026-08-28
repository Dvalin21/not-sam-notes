package com.openlight.notes

import android.content.Context
import com.openlight.notes.db.NotesDatabase
import com.openlight.notes.repository.NoteRepository
import java.io.File

/**
 * Manual DI container (AD-9). No Hilt.
 */
class AppContainer(context: Context) {
    private val database = NotesDatabase.getInstance(context)
    val notesDir: File = File(context.filesDir, "notes").also { it.mkdirs() }
    val repository = NoteRepository(
        noteDao = database.noteDao(),
        notesDir = notesDir
    )
}
