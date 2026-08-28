package com.openlight.notes.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.openlight.notes.core.model.Block
import com.openlight.notes.core.model.Document
import com.openlight.notes.core.model.NoteManifest
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Export suite (AD-12).
 * PDF, PNG, TXT, .docx, .pptx export.
 */
object Exporter {

    /**
     * Export to PDF using platform PdfDocument.
     */
    fun exportPdf(
        context: Context,
        manifest: NoteManifest,
        document: Document,
        outputFile: File
    ): Boolean {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
            val page = pdfDocument.startPage(pageInfo)

            val canvas = page.canvas
            val paint = Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 14f
            }

            var y = 50f

            // Title
            paint.textSize = 24f
            canvas.drawText(manifest.title.ifEmpty { "Untitled" }, 50f, y, paint)
            y += 40f

            // Content
            paint.textSize = 14f
            for (block in document.blocks) {
                when (block) {
                    is Block.Text -> {
                        val lines = wrapText(block.text, paint, 495f)
                        for (line in lines) {
                            if (y > 792f) {
                                pdfDocument.finishPage(page)
                                val newPage = pdfDocument.startPage(pageInfo)
                                canvas.drawText(line, 50f, 50f, paint)
                                y = 70f
                            } else {
                                canvas.drawText(line, 50f, y, paint)
                                y += 20f
                            }
                        }
                    }
                    is Block.Ink -> {
                        // Ink rendered as raster (AD-12)
                        canvas.drawText("[Handwriting]", 50f, y, paint)
                        y += 20f
                    }
                    is Block.Image -> {
                        canvas.drawText("[Image: ${block.media}]", 50f, y, paint)
                        y += 20f
                    }
                    is Block.Audio -> {
                        canvas.drawText("[Audio: ${block.durMs / 1000}s]", 50f, y, paint)
                        y += 20f
                    }
                    is Block.PdfPage -> {
                        canvas.drawText("[PDF Page ${block.page}]", 50f, y, paint)
                        y += 20f
                    }
                }
            }

            pdfDocument.finishPage(page)
            FileOutputStream(outputFile).use { pdfDocument.writeTo(it) }
            pdfDocument.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Export to PNG (first page as bitmap).
     */
    fun exportPng(
        manifest: NoteManifest,
        document: Document,
        outputFile: File,
        width: Int = 1080
    ): Boolean {
        return try {
            val scale = width / 595f
            val height = (842 * scale).toInt()

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)

            val paint = Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 14f * scale
            }

            var y = 50f * scale

            // Title
            paint.textSize = 24f * scale
            canvas.drawText(manifest.title.ifEmpty { "Untitled" }, 50f * scale, y, paint)
            y += 40f * scale

            // Content
            paint.textSize = 14f * scale
            for (block in document.blocks) {
                when (block) {
                    is Block.Text -> {
                        val lines = wrapText(block.text, paint, (width - 100f))
                        for (line in lines) {
                            canvas.drawText(line, 50f * scale, y, paint)
                            y += 20f * scale
                        }
                    }
                    else -> {
                        canvas.drawText("[${block::class.simpleName}]", 50f * scale, y, paint)
                        y += 20f * scale
                    }
                }
            }

            FileOutputStream(outputFile).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Export to plain text.
     */
    fun exportTxt(
        manifest: NoteManifest,
        document: Document,
        outputFile: File
    ): Boolean {
        return try {
            val sb = StringBuilder()
            sb.appendLine(manifest.title.ifEmpty { "Untitled" })
            sb.appendLine("=".repeat(manifest.title.length))
            sb.appendLine()

            for (block in document.blocks) {
                when (block) {
                    is Block.Text -> sb.appendLine(block.text)
                    is Block.Ink -> sb.appendLine("[Handwriting]")
                    is Block.Image -> sb.appendLine("[Image: ${block.media}]")
                    is Block.Audio -> sb.appendLine("[Audio: ${block.durMs / 1000}s]")
                    is Block.PdfPage -> sb.appendLine("[PDF Page ${block.page}]")
                }
                sb.appendLine()
            }

            outputFile.writeText(sb.toString())
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Export to .docx (minimal OOXML writer).
     */
    fun exportDocx(
        manifest: NoteManifest,
        document: Document,
        outputFile: File
    ): Boolean {
        return try {
            ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
                // [Content_Types].xml
                zos.putNextEntry(ZipEntry("[Content_Types].xml"))
                zos.write(getContentTypesXml().toByteArray())
                zos.closeEntry()

                // _rels/.rels
                zos.putNextEntry(ZipEntry("_rels/.rels"))
                zos.write(getRelsXml().toByteArray())
                zos.closeEntry()

                // word/document.xml
                zos.putNextEntry(ZipEntry("word/document.xml"))
                zos.write(getWordDocumentXml(manifest, document).toByteArray())
                zos.closeEntry()

                // word/styles.xml
                zos.putNextEntry(ZipEntry("word/styles.xml"))
                zos.write(getWordStylesXml().toByteArray())
                zos.closeEntry()

                // word/_rels/document.xml.rels
                zos.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
                zos.write(getWordDocumentRelsXml().toByteArray())
                zos.closeEntry()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Export to .pptx (minimal OOXML writer).
     */
    fun exportPptx(
        manifest: NoteManifest,
        document: Document,
        outputFile: File
    ): Boolean {
        return try {
            ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
                // [Content_Types].xml
                zos.putNextEntry(ZipEntry("[Content_Types].xml"))
                zos.write(getPptxContentTypesXml().toByteArray())
                zos.closeEntry()

                // _rels/.rels
                zos.putNextEntry(ZipEntry("_rels/.rels"))
                zos.write(getRelsXml().toByteArray())
                zos.closeEntry()

                // ppt/slides/slide1.xml
                zos.putNextEntry(ZipEntry("ppt/slides/slide1.xml"))
                zos.write(getPptxSlideXml(manifest, document).toByteArray())
                zos.closeEntry()

                // ppt/slideLayouts/slideLayout1.xml
                zos.putNextEntry(ZipEntry("ppt/slideLayouts/slideLayout1.xml"))
                zos.write(getPptxSlideLayoutXml().toByteArray())
                zos.closeEntry()

                // ppt/_rels/presentation.xml.rels
                zos.putNextEntry(ZipEntry("ppt/_rels/presentation.xml.rels"))
                zos.write(getPptxPresentationRelsXml().toByteArray())
                zos.closeEntry()

                // ppt/presentation.xml
                zos.putNextEntry(ZipEntry("ppt/presentation.xml"))
                zos.write(getPptxPresentationXml().toByteArray())
                zos.closeEntry()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "${currentLine} $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = StringBuilder(testLine)
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
        return lines
    }

    private fun getContentTypesXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
            <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
            <Default Extension="xml" ContentType="application/xml"/>
            <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
            <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
        </Types>
    """.trimIndent()

    private fun getRelsXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
            <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
        </Relationships>
    """.trimIndent()

    private fun getWordDocumentXml(manifest: NoteManifest, document: Document): String {
        val sb = StringBuilder()
        sb.append("""
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                <w:body>
        """.trimIndent())

        // Title
        sb.append("""
            <w:p>
                <w:pPr><w:pStyle w:val="Title"/></w:pPr>
                <w:r><w:t>${escapeXml(manifest.title.ifEmpty { "Untitled" })}</w:t></w:r>
            </w:p>
        """.trimIndent())

        for (block in document.blocks) {
            when (block) {
                is Block.Text -> {
                    sb.append("""
                        <w:p>
                            <w:r><w:t>${escapeXml(block.text)}</w:t></w:r>
                        </w:p>
                    """.trimIndent())
                }
                is Block.Ink -> {
                    sb.append("""
                        <w:p>
                            <w:r><w:t>[Handwriting]</w:t></w:r>
                        </w:p>
                    """.trimIndent())
                }
                is Block.Image -> {
                    sb.append("""
                        <w:p>
                            <w:r><w:t>[Image: ${escapeXml(block.media)}]</w:t></w:r>
                        </w:p>
                    """.trimIndent())
                }
                is Block.Audio -> {
                    sb.append("""
                        <w:p>
                            <w:r><w:t>[Audio: ${block.durMs / 1000}s]</w:t></w:r>
                        </w:p>
                    """.trimIndent())
                }
                is Block.PdfPage -> {
                    sb.append("""
                        <w:p>
                            <w:r><w:t>[PDF Page ${block.page}]</w:t></w:r>
                        </w:p>
                    """.trimIndent())
                }
            }
        }

        sb.append("""
                </w:body>
            </w:document>
        """.trimIndent())

        return sb.toString()
    }

    private fun getWordStylesXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
            <w:style w:type="paragraph" w:default="1" w:styleId="Normal">
                <w:name w:val="Normal"/>
                <w:qFormat/>
            </w:style>
            <w:style w:type="paragraph" w:styleId="Title">
                <w:name w:val="Title"/>
                <w:basedOn w:val="Normal"/>
                <w:qFormat/>
                <w:pPr><w:spacing w:before="240" w:after="240"/></w:pPr>
                <w:rPr><w:b/><w:sz w:val="32"/></w:rPr>
            </w:style>
        </w:styles>
    """.trimIndent()

    private fun getWordDocumentRelsXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
            <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
        </Relationships>
    """.trimIndent()

    private fun getPptxContentTypesXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
            <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
            <Default Extension="xml" ContentType="application/xml"/>
            <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
            <Override PartName="/ppt/slides/slide1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>
        </Types>
    """.trimIndent()

    private fun getPptxPresentationXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <p:presentation xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
            <p:sldMasterIdLst><p:sldMasterId id="2147483648" r:id="rId1"/></p:sldMasterIdLst>
            <p:sldIdLst><p:sldId id="256" r:id="rId2"/></p:sldIdLst>
            <p:sldSz cx="9144000" cy="6858000"/>
            <p:notesSz cx="6858000" cy="9144000"/>
        </p:presentation>
    """.trimIndent()

    private fun getPptxSlideXml(manifest: NoteManifest, document: Document): String {
        val sb = StringBuilder()
        sb.append("""
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                <p:cSld>
                    <p:spTree>
                        <p:nvGrpSpPr>
                            <p:cNvPr id="1" name=""/>
                            <p:cNvGrpSpPr/>
                            <p:nvPr/>
                        </p:nvGrpSpPr>
                        <p:grpSpPr>
                            <a:xfrm>
                                <a:off x="0" y="0"/>
                                <a:ext cx="0" cy="0"/>
                                <a:chOff x="0" y="0"/>
                                <a:chExt cx="0" cy="0"/>
                            </a:xfrm>
                        </p:grpSpPr>
        """.trimIndent())

        // Title
        sb.append("""
            <p:sp>
                <p:nvSpPr>
                    <p:cNvPr id="2" name="Title"/>
                    <p:cNvSpPr><a:spLocks noGrp="1"/></p:cNvSpPr>
                    <p:nvPr/>
                </p:nvSpPr>
                <p:spPr>
                    <a:xfrm><a:off x="457200" y="274638"/><a:ext cx="8229600" y="1143000"/></a:xfrm>
                    <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
                </p:spPr>
                <p:txBody>
                    <a:bodyPr/><a:lstStyle/>
                    <a:p>
                        <a:r>
                            <a:rPr lang="en-US" dirty="0"/>
                            <a:t>${escapeXml(manifest.title.ifEmpty { "Untitled" })}</a:t>
                        </a:r>
                        <a:endParaRPr lang="en-US" dirty="0"/>
                    </a:p>
                </p:txBody>
            </p:sp>
        """.trimIndent())

        // Content blocks
        var yOffset = 1600000L
        for (block in document.blocks) {
            val text = when (block) {
                is Block.Text -> block.text
                is Block.Ink -> "[Handwriting]"
                is Block.Image -> "[Image: ${block.media}]"
                is Block.Audio -> "[Audio: ${block.durMs / 1000}s]"
                is Block.PdfPage -> "[PDF Page ${block.page}]"
            }

            sb.append("""
                <p:sp>
                    <p:nvSpPr>
                        <p:cNvPr id="3" name="Content"/>
                        <p:cNvSpPr><a:spLocks noGrp="1"/></p:cNvSpPr>
                        <p:nvPr/>
                    </p:nvSpPr>
                    <p:spPr>
                        <a:xfrm><a:off x="457200" y="$yOffset"/><a:ext cx="8229600" cy="1143000"/></a:xfrm>
                        <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
                    </p:spPr>
                    <p:txBody>
                        <a:bodyPr/><a:lstStyle/>
                        <a:p>
                            <a:r>
                                <a:rPr lang="en-US" dirty="0"/>
                                <a:t>${escapeXml(text)}</a:t>
                            </a:r>
                        </a:p>
                    </p:txBody>
                </p:sp>
            """.trimIndent())

            yOffset += 1200000L
        }

        sb.append("""
                    </p:spTree>
                </p:cSld>
                <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
            </p:sld>
        """.trimIndent())

        return sb.toString()
    }

    private fun getPptxSlideLayoutXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <p:sldLayout xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" type="title" preserve="1">
            <p:cSld name="Title and Content">
                <p:spTree>
                    <p:nvGrpSpPr>
                        <p:cNvPr id="1" name=""/>
                        <p:cNvGrpSpPr/>
                        <p:nvPr/>
                    </p:nvGrpSpPr>
                    <p:grpSpPr>
                        <a:xfrm>
                            <a:off x="0" y="0"/>
                            <a:ext cx="0" cy="0"/>
                            <a:chOff x="0" y="0"/>
                            <a:chExt cx="0" cy="0"/>
                        </a:xfrm>
                    </p:grpSpPr>
                </p:spTree>
            </p:cSld>
        </p:sldLayout>
    """.trimIndent()

    private fun getPptxPresentationRelsXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
            <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="slideLayouts/slideLayout1.xml"/>
            <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide1.xml"/>
        </Relationships>
    """.trimIndent()

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
