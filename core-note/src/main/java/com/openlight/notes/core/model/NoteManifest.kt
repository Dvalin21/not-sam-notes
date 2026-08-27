package com.openlight.notes.core.model

import kotlinx.serialization.Serializable

/**
 * Phase 0: Placeholder document model.
 * Full model will be built in Phase 1.
 */
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
