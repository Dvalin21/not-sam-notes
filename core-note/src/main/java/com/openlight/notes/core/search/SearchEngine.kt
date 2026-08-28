package com.openlight.notes.core.search

/** Search result with snippet. */
data class SearchResult(
    val noteId: String,
    val noteTitle: String,
    val snippet: String,
    val matchType: MatchType
)

enum class MatchType { TITLE, TEXT, HANDWRITING }

/** Search engine: FTS over typed text + recognized handwriting. */
class SearchEngine {
    fun buildQuery(raw: String): String {
        // FTS5 prefix query: each term followed by *
        return raw.trim().split("\\s+".toRegex()).joinToString(" ") { "$it*" }
    }

    fun makeSnippet(content: String, query: String, maxLen: Int = 120): String {
        val idx = content.indexOf(query, ignoreCase = true)
        if (idx < 0) return content.take(maxLen)
        val start = maxOf(0, idx - 40)
        val end = minOf(content.length, idx + query.length + 40)
        return "...${content.substring(start, end)}..."
    }
}
