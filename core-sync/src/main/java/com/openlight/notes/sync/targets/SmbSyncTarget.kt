package com.openlight.notes.sync.targets

import com.openlight.notes.sync.RemoteNote
import com.openlight.notes.sync.SyncTarget
import com.openlight.notes.sync.TargetType

/**
 * SMB sync target (AD-7).
 * Uses smbj for SMB2/3 access.
 * TODO: verify against real SMB server
 */
class SmbSyncTarget(
    override val name: String,
    private val server: String,
    private val share: String,
    private val username: String,
    private val password: String,
    private val domain: String = ""
) : SyncTarget {
    override val id: String = "smb_${server}_${share}"
    override val type: TargetType = TargetType.SMB

    override suspend fun list(): List<RemoteNote> {
        // TODO: implement with verified smbj API
        return emptyList()
    }

    override suspend fun get(id: String): ByteArray? {
        // TODO: implement
        return null
    }

    override suspend fun putAtomic(id: String, bytes: ByteArray) {
        // TODO: implement
    }

    override suspend fun delete(id: String) {
        // TODO: implement
    }

    override suspend fun testConnection(): Boolean {
        return false
    }
}
