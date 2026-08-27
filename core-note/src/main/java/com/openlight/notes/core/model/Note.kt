package com.openlight.notes.core.model

import kotlinx.serialization.Serializable

/** Document block types (AD-2). */
@Serializable
sealed class Block {
    abstract val id: String

    @Serializable
    data class Text(
        override val id: String,
        val text: String = "",
        val spans: List<Span> = emptyList()
    ) : Block()

    @Serializable
    data class Ink(
        override val id: String,
        val height: Float = 900f
    ) : Block()

    @Serializable
    data class Image(
        override val id: String,
        val media: String,
        val w: Int = 0,
        val h: Int = 0
    ) : Block()

    @Serializable
    data class Audio(
        override val id: String,
        val media: String,
        val durMs: Long = 0
    ) : Block()

    @Serializable
    data class PdfPage(
        override val id: String,
        val media: String,
        val page: Int = 0
    ) : Block()
}

@Serializable
data class Span(
    val start: Int,
    val end: Int,
    val style: String // "bold", "italic", "underline", "strike", "h1", "h2", "h3", "bullet", "number", "check", "color:#RRGGBB", "highlight:#RRGGBB"
)

@Serializable
data class Document(
    val blocks: List<Block> = emptyList()
)

@Serializable
data class NoteManifest(
    val format: Int = 1,
    val id: String,
    val title: String = "",
    val folder: String = "/",
    val created: Long = 0L,
    val modified: Long = 0L,
    val favorite: Boolean = false,
    val trashed: Boolean = false,
    val locked: Boolean = false,
    val template: String = "blank",
    val device: String = ""
)
