package com.openlight.notes.sync.targets

import com.openlight.notes.sync.RemoteNote
import com.openlight.notes.sync.SyncTarget
import com.openlight.notes.sync.TargetType
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * WebDAV sync target (AD-7).
 * Minimal client: PROPFIND, GET, PUT, MKCOL, MOVE, DELETE.
 */
class WebDavSyncTarget(
    override val name: String,
    private val baseUrl: String,
    private val username: String,
    private val password: String
) : SyncTarget {
    override val id: String = "webdav_${baseUrl.hashCode()}"
    override val type: TargetType = TargetType.WEBDAV

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun request(method: String, path: String): Request.Builder {
        val builder = Request.Builder()
            .url("$baseUrl/$path")
            .header("Authorization", Credentials.basic(username, password))
        return builder
    }

    override suspend fun list(): List<RemoteNote> {
        val request = Request.Builder()
            .url("$baseUrl/")
            .method("PROPFIND", null)
            .header("Authorization", Credentials.basic(username, password))
            .header("Depth", "1")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return emptyList()

        val body = response.body?.string() ?: return emptyList()
        val results = mutableListOf<RemoteNote>()

        // Simple XML parsing for response
        val hrefPattern = Regex("<d:href>([^<]+)</d:href>")
        val lenPattern = Regex("<d:getcontentlength>(\\d+)</d:getcontentlength>")

        val hrefs = hrefPattern.findAll(body).map { it.groupValues[1] }.toList()
        val lengths = lenPattern.findAll(body).map { it.groupValues[1] }.toList()

        for (i in hrefs.indices) {
            val href = hrefs[i]
            val name = href.substringAfterLast("/")
            if (name.endsWith(".note")) {
                results.add(RemoteNote(
                    id = name.removeSuffix(".note"),
                    size = lengths.getOrNull(i)?.toLongOrNull() ?: 0,
                    stamp = 0
                ))
            }
        }
        return results
    }

    override suspend fun get(id: String): ByteArray? {
        val request = Request.Builder()
            .url("$baseUrl/$id.note")
            .header("Authorization", Credentials.basic(username, password))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return null
        return response.body?.bytes()
    }

    override suspend fun putAtomic(id: String, bytes: ByteArray) {
        // Put temp then move
        val tempId = "${id}_tmp.note"
        val putRequest = Request.Builder()
            .url("$baseUrl/$tempId")
            .put(bytes.toRequestBody("application/octet-stream".toMediaType()))
            .header("Authorization", Credentials.basic(username, password))
            .build()

        client.newCall(putRequest).execute()

        val moveRequest = Request.Builder()
            .url("$baseUrl/$tempId")
            .method("MOVE", null)
            .header("Authorization", Credentials.basic(username, password))
            .header("Destination", "$baseUrl/$id.note")
            .build()

        client.newCall(moveRequest).execute()
    }

    override suspend fun delete(id: String) {
        val request = Request.Builder()
            .url("$baseUrl/$id.note")
            .delete()
            .header("Authorization", Credentials.basic(username, password))
            .build()

        client.newCall(request).execute()
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
