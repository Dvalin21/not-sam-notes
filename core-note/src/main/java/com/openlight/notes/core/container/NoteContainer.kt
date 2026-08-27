package com.openlight.notes.core.container

import com.openlight.notes.core.model.Document
import com.openlight.notes.core.model.NoteManifest
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Atomic note container I/O (AD-1, AD-5).
 * Each note is a zip: manifest.json, document.json, strokes/, media/, recognition/
 */
object NoteContainer {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun create(dir: File, title: String = "Untitled"): File {
        val id = UUID.randomUUID().toString()
        val noteFile = File(dir, "$id.note")
        dir.mkdirs()

        val manifest = NoteManifest(
            id = id,
            title = title,
            created = System.currentTimeMillis(),
            modified = System.currentTimeMillis()
        )
        val document = Document()

        write(noteFile, manifest, document)
        return noteFile
    }

    fun write(file: File, manifest: NoteManifest, document: Document) {
        val tmp = File(file.parentFile, "${file.name}.tmp")
        ZipOutputStream(tmp.outputStream().buffered()).use { zos ->
            zos.putNextEntry(ZipEntry("manifest.json"))
            zos.write(json.encodeToString(NoteManifest.serializer(), manifest).toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("document.json"))
            zos.write(json.encodeToString(Document.serializer(), document).toByteArray())
            zos.closeEntry()
        }
        tmp.setReadable(true, false)
        if (!tmp.renameTo(file)) {
            tmp.delete()
            throw IllegalStateException("Failed to rename ${tmp.name} to ${file.name}")
        }
    }

    fun readManifest(file: File): NoteManifest {
        ZipFile(file).use { zip ->
            val entry = zip.getEntry("manifest.json") ?: throw IllegalStateException("No manifest")
            return json.decodeFromString(NoteManifest.serializer(), zip.getInputStream(entry).bufferedReader().readText())
        }
    }

    fun readDocument(file: File): Document {
        ZipFile(file).use { zip ->
            val entry = zip.getEntry("document.json") ?: return Document()
            return json.decodeFromString(Document.serializer(), zip.getInputStream(entry).bufferedReader().readText())
        }
    }

    fun read(file: File): Pair<NoteManifest, Document> {
        ZipFile(file).use { zip ->
            val manifestEntry = zip.getEntry("manifest.json") ?: throw IllegalStateException("No manifest")
            val manifest = json.decodeFromString(NoteManifest.serializer(), zip.getInputStream(manifestEntry).bufferedReader().readText())

            val docEntry = zip.getEntry("document.json")
            val document = if (docEntry != null) {
                json.decodeFromString(Document.serializer(), zip.getInputStream(docEntry).bufferedReader().readText())
            } else Document()

            return manifest to document
        }
    }

    fun listNotes(dir: File): List<File> {
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.isFile && f.name.endsWith(".note") }?.toList() ?: emptyList()
    }
}
