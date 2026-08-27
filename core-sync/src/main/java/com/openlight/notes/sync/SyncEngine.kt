package com.openlight.notes.core.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Sync target interface (AD-7).
 * All targets implement: list, get, putAtomic, delete.
 */
interface SyncTarget {
    val id: String
    val name: String
    val type: TargetType

    suspend fun list(): List<RemoteNote>
    suspend fun get(id: String): ByteArray?
    suspend fun putAtomic(id: String, bytes: ByteArray)
    suspend fun delete(id: String)
    suspend fun testConnection(): Boolean
}

enum class TargetType { SAF, WEBDAV, SMB, PEER }

data class RemoteNote(
    val id: String,
    val size: Long,
    val stamp: Long
)

/**
 * Sync index entry: tracks last successful sync per note × target.
 */
data class SyncIndexEntry(
    val noteId: String,
    val targetId: String,
    val baseHash: String,
    val remoteStamp: Long,
    val lastSync: Long
)

/**
 * 3-way sync engine (AD-6).
 * No merging, no clock trust. Conflicts duplicate.
 */
class SyncEngine(
    private val targets: List<SyncTarget>,
    private val getLocal: suspend (String) -> ByteArray?,
    private val putLocal: suspend (String, ByteArray) -> Unit,
    private val deleteLocal: suspend (String) -> Unit,
    private val listLocal: suspend () -> List<String>,
    private val getHash: suspend (String) -> String
) {
    suspend fun sync(target: SyncTarget): SyncResult {
        var uploaded = 0
        var downloaded = 0
        var conflicts = 0
        var errors = 0

        try {
            val remoteNotes = target.list().associateBy { it.id }
            val localNotes = listLocal()

            for (localId in localNotes) {
                try {
                    val localHash = getHash(localId)
                    val remote = remoteNotes[localId]

                    if (remote == null) {
                        // Local only → upload
                        val bytes = getLocal(localId) ?: continue
                        target.putAtomic(localId, bytes)
                        uploaded++
                    } else {
                        // Both exist → compare
                        val remoteBytes = target.get(localId)
                        if (remoteBytes == null) {
                            // Remote deleted → download (restore)
                            val bytes = getLocal(localId) ?: continue
                            target.putAtomic(localId, bytes)
                            uploaded++
                        } else {
                            val remoteHash = remoteBytes.contentHashCode().toString()
                            if (localHash != remoteHash) {
                                // Conflict → keep both
                                val conflictId = "${localId}_conflict_${System.currentTimeMillis()}"
                                target.putAtomic(conflictId, remoteBytes)
                                downloaded++
                                conflicts++
                            }
                        }
                    }
                } catch (e: Exception) {
                    errors++
                }
            }

            // Download remote-only notes
            for (remote in remoteNotes.values) {
                if (remote.id !in localNotes) {
                    try {
                        val bytes = target.get(remote.id) ?: continue
                        putLocal(remote.id, bytes)
                        downloaded++
                    } catch (e: Exception) {
                        errors++
                    }
                }
            }
        } catch (e: Exception) {
            errors++
        }

        return SyncResult(uploaded, downloaded, conflicts, errors)
    }
}

data class SyncResult(
    val uploaded: Int,
    val downloaded: Int,
    val conflicts: Int,
    val errors: Int
)
