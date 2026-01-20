package com.ethersense.domain.usecase

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.wifi.WifiManager
import com.ethersense.data.model.LanDevice
import com.ethersense.data.model.LanScanProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import javax.inject.Inject

class LanScannerUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PING_TIMEOUT_MS = 200
        private const val CONCURRENT_SCANS = 50
    }

    fun scan(
        onProgress: suspend (LanScanProgress) -> Unit = {}
    ): Flow<LanDevice> = flow {
        val localIp = getLocalIpAddress() ?: return@flow
        val prefixLength = getPrefixLength() ?: 24 // Default to /24 if unknown
        val ipRange = calculateIpRange(localIp, prefixLength)

        if (ipRange.isEmpty()) return@flow

        var scannedCount = 0
        var foundCount = 0

        ipRange.chunked(CONCURRENT_SCANS).forEach { chunk ->
            coroutineScope {
                val results = chunk.map { ip ->
                    async {
                        scanHost(ip)
                    }
                }.awaitAll()

                results.filterNotNull().forEach { device ->
                    foundCount++
                    emit(device)
                }

                scannedCount += chunk.size
                onProgress(
                    LanScanProgress(
                        currentIp = chunk.last(),
                        scannedCount = scannedCount,
                        totalCount = ipRange.size,
                        foundDevices = foundCount
                    )
                )
            }
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun scanHost(ip: String): LanDevice? {
        return try {
            val startTime = System.currentTimeMillis()
            val address = InetAddress.getByName(ip)

            if (address.isReachable(PING_TIMEOUT_MS)) {
                val responseTime = System.currentTimeMillis() - startTime
                val hostname = try {
                    val canonicalName = address.canonicalHostName
                    if (canonicalName != ip) canonicalName else null
                } catch (e: Exception) {
                    null
                }

                LanDevice(
                    ipAddress = ip,
                    hostname = hostname,
                    isReachable = true,
                    responseTime = responseTime
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getLocalIpAddress(): String? {
        try {
            // Try to get from WifiManager first
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiManager?.connectionInfo?.ipAddress?.let { ipInt ->
                if (ipInt != 0) {
                    return intToIpAddress(ipInt)
                }
            }

            // Fallback to NetworkInterface
            NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { networkInterface ->
                networkInterface.inetAddresses?.toList()?.forEach { address ->
                    if (!address.isLoopbackAddress && address.hostAddress?.contains('.') == true) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return null
    }

    private fun intToIpAddress(ip: Int): String {
        return "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
    }

    private fun getPrefixLength(): Int? {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = connectivityManager?.activeNetwork
            val linkProperties = activeNetwork?.let { connectivityManager.getLinkProperties(it) }

            return linkProperties?.linkAddresses
                ?.firstOrNull { it.address is Inet4Address }
                ?.prefixLength
        } catch (e: Exception) {
            return null
        }
    }

    private fun calculateIpRange(localIp: String, prefixLength: Int): List<String> {
        try {
            val parts = localIp.split(".").map { it.toInt() }
            if (parts.size != 4) return emptyList()

            val localIpInt = (parts[0] shl 24) or (parts[1] shl 16) or (parts[2] shl 8) or parts[3]

            // Calculate network address and broadcast address based on prefix length
            val mask = if (prefixLength == 0) 0 else (-1 shl (32 - prefixLength))
            val networkAddress = localIpInt and mask
            val broadcastAddress = networkAddress or mask.inv()

            // Limit scan to reasonable range (max 1024 hosts to prevent excessive scanning)
            val hostCount = broadcastAddress - networkAddress - 1
            val maxHosts = minOf(hostCount, 1024)

            if (maxHosts <= 0) return emptyList()

            // Generate IP range (excluding network and broadcast addresses)
            return (1..maxHosts).map { offset ->
                val ip = networkAddress + offset
                "${(ip shr 24) and 0xFF}.${(ip shr 16) and 0xFF}.${(ip shr 8) and 0xFF}.${ip and 0xFF}"
            }
        } catch (e: Exception) {
            return emptyList()
        }
    }

    private fun getSubnetPrefix(ip: String, prefixLength: Int = 24): String {
        val parts = ip.split(".")
        return if (parts.size == 4) {
            when {
                prefixLength >= 24 -> "${parts[0]}.${parts[1]}.${parts[2]}"
                prefixLength >= 16 -> "${parts[0]}.${parts[1]}"
                prefixLength >= 8 -> parts[0]
                else -> "0"
            }
        } else {
            "192.168.1"
        }
    }

    fun getLocalNetworkInfo(): Pair<String?, String?> {
        val localIp = getLocalIpAddress()
        val prefixLength = getPrefixLength() ?: 24
        val subnet = localIp?.let { getSubnetPrefix(it, prefixLength) }
        return localIp to subnet
    }
}
