package com.openlight.notes.core.model

import kotlinx.serialization.Serializable

/** Rich text span styles. */
object SpanStyle {
    const val BOLD = "bold"
    const val ITALIC = "italic"
    const val UNDERLINE = "underline"
    const val STRIKETHROUGH = "strike"
    const val H1 = "h1"
    const val H2 = "h2"
    const val H3 = "h3"
    const val BULLET = "bullet"
    const val NUMBER = "number"
    const val CHECK = "check"
    const val CHECKED = "checked"
    fun color(hex: String) = "color:$hex"
    fun highlight(hex: String) = "highlight:$hex"
}

/** A rich text block's spans. */
@Serializable
data class TextBlock(
    val id: String,
    val text: String = "",
    val spans: List<Span> = emptyList()
)

/** Full document: ordered blocks. */
@Serializable
data class NoteDocument(
    val blocks: List<Block> = emptyList()
) {
    fun getTextBlocks(): List<Block.Text> = blocks.filterIsInstance<Block.Text>()
    fun getInkBlocks(): List<Block.Ink> = blocks.filterIsInstance<Block.Ink>()
    fun getImageBlocks(): List<Block.Image> = blocks.filterIsInstance<Block.Image>()
    fun getAudioBlocks(): List<Block.Audio> = blocks.filterIsInstance<Block.Audio>()

    fun addBlock(block: Block): NoteDocument = copy(blocks = blocks + block)
    fun removeBlock(blockId: String): NoteDocument = copy(blocks = blocks.filter { it.id != blockId })
    fun updateBlock(block: Block): NoteDocument = copy(blocks = blocks.map { if (it.id == block.id) block else it })
}
