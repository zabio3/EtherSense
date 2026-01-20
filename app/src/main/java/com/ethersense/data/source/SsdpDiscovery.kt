package com.ethersense.data.source

import com.ethersense.data.model.DiscoveredService
import com.ethersense.data.model.ServiceProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withTimeout
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SsdpDiscovery @Inject constructor() {

    companion object {
        private const val SSDP_ADDRESS = "239.255.255.250"
        private const val SSDP_PORT = 1900
        private const val RECEIVE_TIMEOUT_MS = 3000
        private const val DISCOVERY_TIMEOUT_MS = 10000L

        private val M_SEARCH_REQUEST = """
            M-SEARCH * HTTP/1.1
            HOST: 239.255.255.250:1900
            MAN: "ssdp:discover"
            MX: 3
            ST: ssdp:all

            """.trimIndent().replace("\n", "\r\n")

        private val UPNP_DEVICE_TYPES = mapOf(
            "urn:schemas-upnp-org:device:MediaRenderer" to "Media Renderer",
            "urn:schemas-upnp-org:device:MediaServer" to "Media Server",
            "urn:schemas-upnp-org:device:InternetGatewayDevice" to "Router/Gateway",
            "urn:schemas-upnp-org:device:WANDevice" to "WAN Device",
            "urn:schemas-upnp-org:device:Basic" to "Basic Device",
            "urn:dial-multiscreen-org:service:dial" to "DIAL (Smart TV)",
            "roku:ecp" to "Roku"
        )
    }

    fun discover(): Flow<DiscoveredService> = callbackFlow {
        val socket = DatagramSocket().apply {
            soTimeout = RECEIVE_TIMEOUT_MS
            broadcast = true
        }

        try {
            // Send M-SEARCH request
            val requestBytes = M_SEARCH_REQUEST.toByteArray()
            val address = InetAddress.getByName(SSDP_ADDRESS)
            val sendPacket = DatagramPacket(requestBytes, requestBytes.size, address, SSDP_PORT)
            socket.send(sendPacket)

            // Receive responses
            val buffer = ByteArray(4096)
            val discoveredLocations = mutableSetOf<String>()

            withTimeout(DISCOVERY_TIMEOUT_MS) {
                while (true) {
                    try {
                        val receivePacket = DatagramPacket(buffer, buffer.size)
                        socket.receive(receivePacket)

                        val response = String(receivePacket.data, 0, receivePacket.length)
                        val service = parseResponse(response, receivePacket.address.hostAddress ?: "")

                        // Avoid duplicates based on location
                        val location = extractHeader(response, "LOCATION")
                        if (location != null && !discoveredLocations.contains(location)) {
                            discoveredLocations.add(location)
                            service?.let { trySend(it) }
                        }
                    } catch (e: SocketTimeoutException) {
                        // Timeout, try receiving more
                        break
                    }
                }
            }
        } catch (e: Exception) {
            // Discovery ended
        } finally {
            socket.close()
        }

        awaitClose {
            if (!socket.isClosed) {
                socket.close()
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun parseResponse(response: String, hostAddress: String): DiscoveredService? {
        val st = extractHeader(response, "ST") ?: extractHeader(response, "NT")
        val usn = extractHeader(response, "USN")
        val location = extractHeader(response, "LOCATION")
        val server = extractHeader(response, "SERVER")

        val deviceName = usn?.let { extractDeviceName(it) }
            ?: server
            ?: getDeviceTypeName(st)
            ?: "Unknown Device"

        val txtRecords = mutableMapOf<String, String>()
        location?.let { txtRecords["LOCATION"] = it }
        server?.let { txtRecords["SERVER"] = it }
        usn?.let { txtRecords["USN"] = it }
        st?.let { txtRecords["ST"] = it }

        val port = location?.let { extractPortFromUrl(it) }

        return DiscoveredService(
            name = deviceName,
            type = st ?: "upnp:rootdevice",
            host = hostAddress,
            port = port,
            txtRecords = txtRecords,
            protocol = ServiceProtocol.SSDP
        )
    }

    private fun extractHeader(response: String, header: String): String? {
        val pattern = Regex("""$header:\s*(.+)""", RegexOption.IGNORE_CASE)
        return pattern.find(response)?.groupValues?.get(1)?.trim()
    }

    private fun extractDeviceName(usn: String): String? {
        // USN format: uuid:device-UUID::urn:schemas-upnp-org:device:deviceType:version
        val uuidPattern = Regex("""uuid:([^:]+)""")
        return uuidPattern.find(usn)?.groupValues?.get(1)?.take(8)?.let { "Device-$it" }
    }

    private fun getDeviceTypeName(st: String?): String? {
        if (st == null) return null

        for ((pattern, name) in UPNP_DEVICE_TYPES) {
            if (st.contains(pattern, ignoreCase = true)) {
                return name
            }
        }
        return null
    }

    private fun extractPortFromUrl(url: String): Int? {
        return try {
            val pattern = Regex("""https?://[^:/]+:(\d+)""")
            pattern.find(url)?.groupValues?.get(1)?.toInt()
        } catch (e: Exception) {
            null
        }
    }
}
