package com.openlight.notes.core.test

import com.openlight.notes.core.container.NoteContainer
import com.openlight.notes.core.ink.Brush
import com.openlight.notes.core.ink.Stroke
import com.openlight.notes.core.model.Document
import com.openlight.notes.core.model.NoteManifest
import org.junit.jupiter.api.Test
import java.io.File
import java.util.UUID
import kotlin.system.measureTimeMillis

class PerformanceTest {

    private fun generateStroke(seed: Int): Stroke {
        val points = mutableListOf<FloatArray>()
        val baseX = (seed * 7) % 1000
        val baseY = (seed * 13) % 1800
        for (i in 0 until 50) {
            points.add(
                floatArrayOf(
                    baseX + i * 2f,
                    baseY + (Math.sin(i * 0.3) * 30).toFloat(),
                    (seed * 1000 + i * 17).toFloat(),
                    0.5f,
                    0f,
                    0f
                )
            )
        }
        return Stroke(brush = Brush("pen", "#1A1A1A", 3.5f), points = points)
    }

    @Test
    fun test5KStrokePage() {
        val notesDir = File("build/test-notes-5k").also { it.mkdirs() }
        val strokeTime = measureTimeMillis {
            val noteId = UUID.randomUUID().toString()
            val noteFile = File(notesDir, "$noteId.note")
            val manifest = NoteManifest(id = noteId, title = "5K Stroke Test")

            val blocks = mutableListOf<com.openlight.notes.core.model.Block>()
            val strokesPerBlock = 500
            val blockCount = 10

            for (b in 0 until blockCount) {
                blocks.add(com.openlight.notes.core.model.Block.Ink(id = "block_$b", height = 900f))
            }

            val document = Document(blocks = blocks)
            NoteContainer.write(noteFile, manifest, document)

            for (b in 0 until blockCount) {
                val blockId = "block_$b"
                val strokes = mutableListOf<Stroke>()
                for (s in 0 until strokesPerBlock) {
                    strokes.add(generateStroke(b * strokesPerBlock + s))
                }
                NoteContainer.writeStrokes(noteFile, blockId, strokes)
            }

            val readDoc = NoteContainer.readDocument(noteFile)
            val totalStrokes = readDoc.blocks.sumOf { block ->
                if (block is com.openlight.notes.core.model.Block.Ink) {
                    NoteContainer.readStrokes(noteFile, block.id).size
                } else 0
            }

            println("5K stroke test: ${readDoc.blocks.size} blocks, $totalStrokes strokes total")
            println("Note file size: ${noteFile.length() / 1024} KB")
        }
        println("5K stroke page creation: ${strokeTime} ms")
        org.junit.jupiter.api.Assertions.assertTrue(strokeTime < 10000, "5K stroke creation too slow: ${strokeTime}ms")
    }

    @Test
    fun test2KNoteStore() {
        val notesDir = File("build/test-notes-2k").also { it.mkdirs() }
        val storeTime = measureTimeMillis {
            for (i in 0 until 2000) {
                val noteId = UUID.randomUUID().toString()
                val noteFile = File(notesDir, "$noteId.note")
                val manifest = NoteManifest(
                    id = noteId,
                    title = "Note $i",
                    folder = "/Test/${i % 10}"
                )
                val document = Document(
                    blocks = listOf(
                        com.openlight.notes.core.model.Block.Text(
                            id = "text_$i",
                            text = "This is test note number $i with some content"
                        )
                    )
                )
                NoteContainer.write(noteFile, manifest, document)
            }

            val count = NoteContainer.listNotes(notesDir).size
            println("2K note store test: $count notes created")
        }
        println("2K note store creation: ${storeTime} ms")
        org.junit.jupiter.api.Assertions.assertTrue(storeTime < 30000, "2K note creation too slow: ${storeTime}ms")
    }
}
