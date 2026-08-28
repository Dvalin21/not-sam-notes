package com.openlight.notes.sync.peer

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.UUID
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Peer sync server (AD-10).
 * Exposes list/get/put/delete as four fixed HTTP routes over TLS.
 */
class PeerSyncServer(
    private val context: Context,
    private val port: Int = 0 // 0 = random
) : NanoHTTPD("0.0.0.0", port) {

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var actualPort: Int = port

    var onList: (() -> List<String>)? = null
    var onGet: ((String) -> ByteArray?)? = null
    var onPut: ((String, ByteArray) -> Unit)? = null
    var onDelete: ((String) -> Unit)? = null

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        return when {
            uri == "/list" && method == Method.GET -> {
                val notes = onList?.let { it() } ?: emptyList()
                newFixedLengthResponse(Response.Status.OK, "application/json", notes.toString())
            }
            uri.startsWith("/note/") && method == Method.GET -> {
                val id = uri.removePrefix("/note/")
                val bytes = onGet?.let { it(id) }
                if (bytes != null) {
                    newFixedLengthResponse(Response.Status.OK, "application/octet-stream", bytes.inputStream(), bytes.size.toLong())
                } else {
                    newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found")
                }
            }
            uri.startsWith("/note/") && method == Method.PUT -> {
                val id = uri.removePrefix("/note/")
                val body = HashMap<String, String>()
                session.parseBody(body)
                val bytes = body["postData"]?.toByteArray() ?: ByteArray(0)
                onPut?.invoke(id, bytes)
                newFixedLengthResponse(Response.Status.OK, "text/plain", "OK")
            }
            uri.startsWith("/note/") && method == Method.DELETE -> {
                val id = uri.removePrefix("/note/")
                onDelete?.invoke(id)
                newFixedLengthResponse(Response.Status.OK, "text/plain", "OK")
            }
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found")
        }
    }

    fun startServer() {
        try {
            start()
            actualPort = listeningPort
            _isRunning.value = true
            Log.i("PeerSyncServer", "Server started on port $actualPort")
        } catch (e: IOException) {
            Log.e("PeerSyncServer", "Failed to start: ${e.message}")
        }
    }

    fun stopServer() {
        stop()
        _isRunning.value = false
    }
}

/**
 * Peer discovery using NSD (mDNS/DNS-SD).
 */
class PeerDiscovery(
    private val context: Context
) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val SERVICE_TYPE = "_opennotes._tcp"

    fun registerService(port: Int) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "NotSamNotes-${UUID.randomUUID().toString().take(8)}"
            serviceType = SERVICE_TYPE
            setPort(port)
        }
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {}
            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
            override fun onServiceUnregistered(info: NsdServiceInfo) {}
            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
        })
    }

    fun discoverPeers(listener: (String, Int) -> Unit) {
        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}
            override fun onServiceFound(service: NsdServiceInfo) {
                nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        listener(serviceInfo.serviceName, serviceInfo.port)
                    }
                })
            }
            override fun onServiceLost(service: NsdServiceInfo) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }
}

/**
 * Peer sync client.
 */
class PeerSyncClient(
    private val host: String,
    private val port: Int
) {
    private val baseUrl = "https://$host:$port"

    suspend fun list(): List<String> {
        // TODO: implement HTTPS call with pinned cert
        return emptyList()
    }

    suspend fun get(id: String): ByteArray? {
        // TODO: implement HTTPS call
        return null
    }

    suspend fun put(id: String, bytes: ByteArray) {
        // TODO: implement HTTPS call
    }

    suspend fun delete(id: String) {
        // TODO: implement HTTPS call
    }
}
