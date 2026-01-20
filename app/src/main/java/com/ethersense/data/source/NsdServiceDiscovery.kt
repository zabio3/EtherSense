package com.ethersense.data.source

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.ethersense.data.model.DiscoveredService
import com.ethersense.data.model.ServiceProtocol
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NsdServiceDiscovery @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val nsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    companion object {
        val COMMON_SERVICE_TYPES = listOf(
            "_http._tcp." to "HTTP",
            "_https._tcp." to "HTTPS",
            "_airplay._tcp." to "AirPlay",
            "_raop._tcp." to "AirPlay (Audio)",
            "_googlecast._tcp." to "Chromecast",
            "_spotify-connect._tcp." to "Spotify Connect",
            "_ipp._tcp." to "Internet Printing",
            "_printer._tcp." to "Printer",
            "_smb._tcp." to "SMB/Samba",
            "_afpovertcp._tcp." to "AFP (Apple Filing)",
            "_ssh._tcp." to "SSH",
            "_sftp-ssh._tcp." to "SFTP",
            "_ftp._tcp." to "FTP",
            "_nfs._tcp." to "NFS",
            "_daap._tcp." to "iTunes/DAAP",
            "_homekit._tcp." to "HomeKit",
            "_hap._tcp." to "HomeKit Accessory",
            "_companion-link._tcp." to "Apple Companion"
        )
    }

    fun discoverServices(serviceType: String): Flow<DiscoveredService> = callbackFlow {
        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                // Discovery started
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                resolveService(service) { resolvedService ->
                    resolvedService?.let { trySend(it) }
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                // Service lost - could emit a removal event if needed
            }

            override fun onDiscoveryStopped(serviceType: String) {
                // Discovery stopped
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                close(Exception("Discovery failed: $errorCode"))
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                // Stop failed
            }
        }

        try {
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            close(e)
        }

        awaitClose {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener)
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }

    fun discoverAllCommonServices(): Flow<DiscoveredService> = callbackFlow {
        val activeListeners = mutableListOf<NsdManager.DiscoveryListener>()

        for ((serviceType, _) in COMMON_SERVICE_TYPES) {
            val listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(regType: String) {}

                override fun onServiceFound(service: NsdServiceInfo) {
                    resolveService(service) { resolvedService ->
                        resolvedService?.let { trySend(it) }
                    }
                }

                override fun onServiceLost(service: NsdServiceInfo) {}
                override fun onDiscoveryStopped(serviceType: String) {}
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            }

            try {
                nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
                activeListeners.add(listener)
            } catch (e: Exception) {
                // Some service types might fail, continue with others
            }
        }

        awaitClose {
            activeListeners.forEach { listener ->
                try {
                    nsdManager.stopServiceDiscovery(listener)
                } catch (e: Exception) {
                    // Ignore cleanup errors
                }
            }
        }
    }

    private fun resolveService(
        serviceInfo: NsdServiceInfo,
        callback: (DiscoveredService?) -> Unit
    ) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                callback(null)
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                val txtRecords = mutableMapOf<String, String>()

                // Extract TXT records if available (API 21+)
                try {
                    serviceInfo.attributes?.forEach { (key, value) ->
                        txtRecords[key] = value?.let { String(it) } ?: ""
                    }
                } catch (e: Exception) {
                    // Ignore
                }

                callback(
                    DiscoveredService(
                        name = serviceInfo.serviceName,
                        type = serviceInfo.serviceType,
                        host = serviceInfo.host?.hostAddress,
                        port = serviceInfo.port,
                        txtRecords = txtRecords,
                        protocol = ServiceProtocol.MDNS
                    )
                )
            }
        }

        try {
            nsdManager.resolveService(serviceInfo, resolveListener)
        } catch (e: Exception) {
            callback(null)
        }
    }

    fun getServiceTypeName(serviceType: String): String {
        return COMMON_SERVICE_TYPES.find { it.first == serviceType }?.second
            ?: serviceType.removePrefix("_").removeSuffix("._tcp.").removeSuffix("._udp.")
    }
}
