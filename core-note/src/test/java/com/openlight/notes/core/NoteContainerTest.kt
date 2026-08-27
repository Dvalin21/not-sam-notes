package com.openlight.notes.core

import com.openlight.notes.core.container.NoteContainer
import com.openlight.notes.core.model.Document
import com.openlight.notes.core.model.NoteManifest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class NoteContainerTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `create note generates valid zip`() {
        val file = NoteContainer.create(tempDir, "Test Note")
        assertTrue(file.exists())
        assertTrue(file.name.endsWith(".note"))

        val manifest = NoteContainer.readManifest(file)
        assertEquals("Test Note", manifest.title)
        assertEquals(1, manifest.format)
    }

    @Test
    fun `write and read round-trip`() {
        val file = NoteContainer.create(tempDir, "Round Trip")
        val manifest = NoteContainer.readManifest(file).copy(title = "Updated")
        val document = Document()

        NoteContainer.write(file, manifest, document)

        val (readManifest, readDoc) = NoteContainer.read(file)
        assertEquals("Updated", readManifest.title)
        assertTrue(readDoc.blocks.isEmpty())
    }

    @Test
    fun `list notes returns all note files`() {
        NoteContainer.create(tempDir, "Note 1")
        NoteContainer.create(tempDir, "Note 2")
        NoteContainer.create(tempDir, "Note 3")

        val notes = NoteContainer.listNotes(tempDir)
        assertEquals(3, notes.size)
    }

    @Test
    fun `atomic write - no temp file left behind`() {
        val file = NoteContainer.create(tempDir, "Atomic")
        val manifest = NoteContainer.readManifest(file)
        NoteContainer.write(file, manifest.copy(title = "Rewritten"), Document())

        val tempFiles = tempDir.listFiles { f -> f.name.endsWith(".tmp") }
        assertTrue(tempFiles.isNullOrEmpty(), "Temp files should be cleaned up")
    }
}
