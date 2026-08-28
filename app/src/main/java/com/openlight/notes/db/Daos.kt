package com.openlight.notes.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE trashed = 0 ORDER BY modified DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE trashed = 1 ORDER BY modified DESC")
    fun observeTrashed(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE folder = :folder AND trashed = 0 ORDER BY modified DESC")
    fun observeByFolder(folder: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(notes: List<NoteEntity>)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM notes")
    suspend fun getAll(): List<NoteEntity>

    @Query("DELETE FROM notes")
    suspend fun clear()

    @Query("SELECT * FROM notes WHERE trashed = 0 AND (title LIKE '%' || :query || '%' OR folder LIKE '%' || :query || '%')")
    suspend fun search(query: String): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE trashed = 0 AND favorite = 1 ORDER BY modified DESC")
    fun observeFavorites(): Flow<List<NoteEntity>>

    @Query("UPDATE notes SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query("UPDATE notes SET trashed = :trashed WHERE id = :id")
    suspend fun setTrashed(id: String, trashed: Boolean)

    @Query("UPDATE notes SET folder = :folder WHERE id = :id")
    suspend fun setFolder(id: String, folder: String)

    @Query("UPDATE notes SET locked = :locked WHERE id = :id")
    suspend fun setLocked(id: String, locked: Boolean)
}
