package com.openlight.notes.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.openlight.notes.core.model.Block
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * PDF import (Phase 9).
 * PdfRenderer pages → pdfPage blocks + ink overlay.
 */
object PdfImporter {

    /**
     * Import a PDF from a URI, returning a list of pdfPage blocks.
     */
    fun importPdf(context: Context, pdfUri: Uri, mediaDir: File): List<Block.PdfPage> {
        val blocks = mutableListOf<Block.PdfPage>()
        mediaDir.mkdirs()

        val fileDescriptor = context.contentResolver.openFileDescriptor(pdfUri, "r")
            ?: return blocks

        val renderer = PdfRenderer(fileDescriptor)
        try {
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)

                // Render page to bitmap at 2x for quality
                val width = page.width * 2
                val height = page.height * 2
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                // Save as PNG
                val mediaId = UUID.randomUUID().toString()
                val mediaFile = File(mediaDir, "$mediaId.png")
                FileOutputStream(mediaFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                blocks.add(
                    Block.PdfPage(
                        id = "pdfpage_${i + 1}",
                        media = "media/${mediaFile.name}",
                        page = i + 1
                    )
                )

                bitmap.recycle()
                page.close()
            }
        } finally {
            renderer.close()
            fileDescriptor.close()
        }

        return blocks
    }

    /**
     * Import a PDF from a file.
     */
    fun importPdfFromFile(file: File, mediaDir: File): List<Block.PdfPage> {
        val blocks = mutableListOf<Block.PdfPage>()
        mediaDir.mkdirs()

        val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fileDescriptor)
        try {
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val width = page.width * 2
                val height = page.height * 2
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                val mediaId = UUID.randomUUID().toString()
                val mediaFile = File(mediaDir, "$mediaId.png")
                FileOutputStream(mediaFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                blocks.add(
                    Block.PdfPage(
                        id = "pdfpage_${i + 1}",
                        media = "media/${mediaFile.name}",
                        page = i + 1
                    )
                )

                bitmap.recycle()
                page.close()
            }
        } finally {
            renderer.close()
            fileDescriptor.close()
        }

        return blocks
    }

    /**
     * Get page count without rendering.
     */
    fun getPageCount(file: File): Int {
        val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fileDescriptor)
        return try {
            renderer.pageCount
        } finally {
            renderer.close()
            fileDescriptor.close()
        }
    }
}
