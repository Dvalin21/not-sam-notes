package com.openlight.notes.core.sync.targets

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.openlight.notes.core.sync.RemoteNote
import com.openlight.notes.core.sync.SyncTarget
import com.openlight.notes.core.sync.TargetType
import java.io.File

/**
 * SAF (Storage Access Framework) sync target.
 * Uses Android's DocumentsProvider for local/USB folders.
 */
class SafSyncTarget(
    override val name: String,
    private val treeUri: Uri,
    private val context: Context
) : SyncTarget {
    override val id: String = "saf_${treeUri.hashCode()}"
    override val type: TargetType = TargetType.SAF

    override suspend fun list(): List<RemoteNote> {
        val results = mutableListOf<RemoteNote>()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
        context.contentResolver.query(childrenUri, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_SIZE, DocumentsContract.Document.COLUMN_LAST_MODIFIED), null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(1)
                if (name.endsWith(".note")) {
                    results.add(RemoteNote(
                        id = name.removeSuffix(".note"),
                        size = cursor.getLong(2),
                        stamp = cursor.getLong(3)
                    ))
                }
            }
        }
        return results
    }

    override suspend fun get(id: String): ByteArray? {
        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, "${DocumentsContract.getTreeDocumentId(treeUri)}/$id.note")
        return context.contentResolver.openInputStream(docUri)?.readBytes()
    }

    override suspend fun putAtomic(id: String, bytes: ByteArray) {
        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, "${DocumentsContract.getTreeDocumentId(treeUri)}/$id.note")
        context.contentResolver.openOutputStream(docUri, "w")?.use { it.write(bytes) }
    }

    override suspend fun delete(id: String) {
        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, "${DocumentsContract.getTreeDocumentId(treeUri)}/$id.note")
        DocumentsContract.deleteDocument(context.contentResolver, docUri)
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
