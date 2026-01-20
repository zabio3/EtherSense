package com.ethersense.domain.usecase

import android.content.Context
import android.net.ConnectivityManager
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
        val subnet = getSubnetPrefix(localIp)

        val ipRange = (1..254).map { "$subnet.$it" }
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

    private fun getSubnetPrefix(ip: String): String {
        val parts = ip.split(".")
        return if (parts.size == 4) {
            "${parts[0]}.${parts[1]}.${parts[2]}"
        } else {
            "192.168.1"
        }
    }

    fun getLocalNetworkInfo(): Pair<String?, String?> {
        val localIp = getLocalIpAddress()
        val subnet = localIp?.let { getSubnetPrefix(it) }
        return localIp to subnet
    }
}
