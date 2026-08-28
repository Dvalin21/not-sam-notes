package com.openlight.notes.repository

import com.openlight.notes.core.container.NoteContainer
import com.openlight.notes.db.NoteDao
import com.openlight.notes.db.NoteEntity
import com.openlight.notes.core.model.Document
import com.openlight.notes.core.model.NoteManifest
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Repository: single source of truth for note data.
 * Room is the index; files are the source of truth (AD-1).
 */
class NoteRepository(
    private val noteDao: NoteDao,
    private val notesDir: File
) {
    fun observeNotes(): Flow<List<NoteEntity>> = noteDao.observeAll()
    fun observeTrashed(): Flow<List<NoteEntity>> = noteDao.observeTrashed()
    fun observeFolder(folder: String): Flow<List<NoteEntity>> = noteDao.observeByFolder(folder)

    suspend fun getNote(id: String): NoteEntity? = noteDao.getById(id)

    suspend fun createNote(title: String = "Untitled", folder: String = "/"): String {
        val file = NoteContainer.create(File(notesDir, folder.removeSuffix("/")), title)
        val manifest = NoteContainer.readManifest(file)
        val entity = manifest.toEntity(file.absolutePath)
        noteDao.upsert(entity)
        return manifest.id
    }

    suspend fun createNoteWithId(id: String, title: String = "Untitled", folder: String = "/"): String {
        val file = File(notesDir, "$id.note")
        notesDir.mkdirs()
        val manifest = NoteManifest(
            id = id,
            title = title,
            created = System.currentTimeMillis(),
            modified = System.currentTimeMillis()
        )
        val document = Document()
        NoteContainer.write(file, manifest, document)
        val entity = manifest.toEntity(file.absolutePath)
        noteDao.upsert(entity)
        return id
    }

    suspend fun saveNote(id: String, manifest: NoteManifest, document: Document) {
        val entity = noteDao.getById(id) ?: return
        val file = File(entity.filePath)
        val updatedManifest = manifest.copy(modified = System.currentTimeMillis())
        NoteContainer.write(file, updatedManifest, document)
        noteDao.upsert(updatedManifest.toEntity(file.absolutePath))
    }

    suspend fun deleteNote(id: String) {
        val entity = noteDao.getById(id) ?: return
        File(entity.filePath).delete()
        noteDao.deleteById(id)
    }

    suspend fun search(query: String): List<NoteEntity> {
        return noteDao.search(query)
    }

    suspend fun setFavorite(id: String, favorite: Boolean) {
        noteDao.setFavorite(id, favorite)
    }

    suspend fun setTrashed(id: String, trashed: Boolean) {
        noteDao.setTrashed(id, trashed)
    }

    suspend fun setFolder(id: String, folder: String) {
        noteDao.setFolder(id, folder)
    }

    suspend fun setLocked(id: String, locked: Boolean) {
        noteDao.setLocked(id, locked)
    }

    fun observeFavorites(): Flow<List<NoteEntity>> = noteDao.observeFavorites()

    suspend fun toggleFavorite(id: String) {
        val entity = noteDao.getById(id) ?: return
        noteDao.setFavorite(id, !entity.favorite)
    }

    suspend fun trashNote(id: String) {
        noteDao.setTrashed(id, true)
    }

    suspend fun restoreNote(id: String) {
        noteDao.setTrashed(id, false)
    }

    suspend fun deletePermanently(id: String) {
        val entity = noteDao.getById(id) ?: return
        File(entity.filePath).delete()
        noteDao.deleteById(id)
    }

    suspend fun rebuildIndex() {
        noteDao.clear()
        val files = NoteContainer.listNotes(notesDir)
        for (file in files) {
            try {
                val manifest = NoteContainer.readManifest(file)
                noteDao.upsert(manifest.toEntity(file.absolutePath))
            } catch (e: Exception) {
                // Skip corrupt files
            }
        }
    }
}

private fun NoteManifest.toEntity(path: String) = NoteEntity(
    id = id,
    title = title,
    folder = folder,
    created = created,
    modified = modified,
    favorite = favorite,
    trashed = trashed,
    locked = locked,
    template = template,
    device = device,
    filePath = path
)
