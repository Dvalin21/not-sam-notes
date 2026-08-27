package com.openlight.notes.core.sync.targets

import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare
import com.openlight.notes.core.sync.RemoteNote
import com.openlight.notes.core.sync.SyncTarget
import com.openlight.notes.core.sync.TargetType
import java.io.ByteArrayOutputStream

/**
 * SMB sync target (AD-7).
 * Uses smbj for SMB2/3 access.
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

    private val client = SMBClient()

    override suspend fun list(): List<RemoteNote> {
        val results = mutableListOf<RemoteNote>()
        val connection = client.connect(server)
        try {
            val ac = AuthenticationContext(username, password.toCharArray(), domain)
            val session = connection.authenticate(ac)
            val shareObj = session.connectShare(share) as DiskShare

            for (file in shareObj.list("")) {
                if (file.fileName.endsWith(".note")) {
                    results.add(RemoteNote(
                        id = file.fileName.removeSuffix(".note"),
                        size = file.endOfFile,
                        stamp = file.lastWriteTime.toEpochMillis()
                    ))
                }
            }
        } finally {
            connection.close()
        }
        return results
    }

    override suspend fun get(id: String): ByteArray? {
        val connection = client.connect(server)
        try {
            val ac = AuthenticationContext(username, password.toCharArray(), domain)
            val session = connection.authenticate(ac)
            val shareObj = session.connectShare(share) as DiskShare

            val file = shareObj.openFile(id + ".note")
            val baos = ByteArrayOutputStream()
            file.read(baos)
            return baos.toByteArray()
        } catch (e: Exception) {
            return null
        } finally {
            connection.close()
        }
    }

    override suspend fun putAtomic(id: String, bytes: ByteArray) {
        val connection = client.connect(server)
        try {
            val ac = AuthenticationContext(username, password.toCharArray(), domain)
            val session = connection.authenticate(ac)
            val shareObj = session.connectShare(share) as DiskShare

            val tempName = "${id}_tmp.note"
            val tempFile = shareObj.openFile(tempName, setOf(com.hierynomus.msfscc.file.FileAccess.GENERIC_WRITE))
            tempFile.write(bytes, 0)
            tempFile.close()

            shareObj.rename(tempName, id + ".note")
        } finally {
            connection.close()
        }
    }

    override suspend fun delete(id: String) {
        val connection = client.connect(server)
        try {
            val ac = AuthenticationContext(username, password.toCharArray(), domain)
            val session = connection.authenticate(ac)
            val shareObj = session.connectShare(share) as DiskShare

            shareObj.rm(id + ".note")
        } finally {
            connection.close()
        }
    }

    override suspend fun testConnection(): Boolean {
        return try {
            list()
            true
        } catch (e: Exception) {
            false
        }
    }
}
