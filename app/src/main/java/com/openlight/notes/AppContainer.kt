package com.openlight.notes

import android.content.Context
import android.os.Environment
import com.openlight.notes.core.db.NotesDatabase
import com.openlight.notes.core.repository.NoteRepository
import java.io.File

/**
 * Manual DI container (AD-9). No Hilt.
 */
class AppContainer(context: Context) {
    private val database = NotesDatabase.getInstance(context)
    val notesDir: File = File(context.filesDir, "notes").also { it.mkdirs() }
    val repository = NoteRepository(
        noteDao = database.noteDao(),
        ftsDao = database.noteFtsDao(),
        notesDir = notesDir
    )
}
