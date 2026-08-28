package com.openlight.notes.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val folder: String,
    val created: Long,
    val modified: Long,
    val favorite: Boolean,
    val trashed: Boolean,
    val locked: Boolean,
    val template: String,
    val device: String,
    val filePath: String
)
