package com.ethersense.data.source

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import com.ethersense.data.model.NetworkInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkInfoDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wifiManager: WifiManager
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val connectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    @Suppress("DEPRECATION")
    suspend fun getNetworkInfo(): NetworkInfo = withContext(Dispatchers.IO) {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
        val linkProperties = activeNetwork?.let { connectivityManager.getLinkProperties(it) }

        val isWifiConnected = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isCellularConnected = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

        // Get WifiInfo - use TransportInfo on API 31+, fallback to deprecated method
        val wifiInfo: WifiInfo? = if (isWifiConnected) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // On Android 12+, try to get WifiInfo from NetworkCapabilities
                (capabilities?.transportInfo as? WifiInfo)
                    ?: wifiManager.connectionInfo // Fallback to deprecated method
            } else {
                wifiManager.connectionInfo
            }
        } else null

        val localIp = getLocalIpAddress(linkProperties)
        val gateway = getGatewayAddress(linkProperties)
        val dnsServers = getDnsServers(linkProperties)
        val subnetMask = getSubnetMask(linkProperties)

        val externalIp = try {
            fetchExternalIp()
        } catch (e: Exception) {
            null
        }

        NetworkInfo(
            localIpAddress = localIp,
            subnetMask = subnetMask,
            gateway = gateway,
            dnsServers = dnsServers,
            externalIpAddress = externalIp,
            ssid = if (isWifiConnected) wifiInfo?.ssid?.removeSurrounding("\"") else null,
            bssid = if (isWifiConnected) wifiInfo?.bssid else null,
            linkSpeed = if (isWifiConnected) wifiInfo?.linkSpeed else null,
            frequency = if (isWifiConnected) wifiInfo?.frequency else null,
            rssi = if (isWifiConnected) wifiInfo?.rssi else null,
            networkType = getNetworkType(capabilities),
            isWifiConnected = isWifiConnected,
            isCellularConnected = isCellularConnected
        )
    }

    @Suppress("DEPRECATION")
    private fun getLocalIpAddress(linkProperties: LinkProperties?): String? {
        try {
            // Try LinkProperties first (preferred on newer APIs)
            linkProperties?.linkAddresses?.forEach { linkAddress ->
                if (linkAddress.address is Inet4Address && !linkAddress.address.isLoopbackAddress) {
                    return linkAddress.address.hostAddress
                }
            }

            // Fallback to WifiManager (deprecated on API 31+)
            val wifiInfo = wifiManager.connectionInfo
            val ipInt = wifiInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                return intToIpAddress(ipInt)
            }

            // Final fallback to NetworkInterface
            NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { networkInterface ->
                networkInterface.inetAddresses?.toList()?.forEach { address ->
                    if (!address.isLoopbackAddress && address is Inet4Address) {
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

    private fun getGatewayAddress(linkProperties: LinkProperties?): String? {
        return linkProperties?.routes
            ?.firstOrNull { it.isDefaultRoute && it.gateway != null }
            ?.gateway
            ?.hostAddress
    }

    private fun getDnsServers(linkProperties: LinkProperties?): List<String> {
        return linkProperties?.dnsServers
            ?.mapNotNull { it.hostAddress }
            ?: emptyList()
    }

    private fun getSubnetMask(linkProperties: LinkProperties?): String? {
        val prefixLength = linkProperties?.linkAddresses
            ?.firstOrNull { it.address is Inet4Address }
            ?.prefixLength
            ?: return null

        return prefixLengthToSubnetMask(prefixLength)
    }

    private fun prefixLengthToSubnetMask(prefixLength: Int): String {
        val mask = -0x1 shl (32 - prefixLength)
        return "${mask shr 24 and 0xFF}.${mask shr 16 and 0xFF}.${mask shr 8 and 0xFF}.${mask and 0xFF}"
    }

    private fun getNetworkType(capabilities: NetworkCapabilities?): String {
        return when {
            capabilities == null -> "Unknown"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "Unknown"
        }
    }

    private fun fetchExternalIp(): String? {
        val services = listOf(
            "https://api.ipify.org",
            "https://icanhazip.com",
            "https://checkip.amazonaws.com"
        )

        for (service in services) {
            try {
                val request = Request.Builder()
                    .url(service)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        return response.body?.string()?.trim()
                    }
                }
            } catch (e: Exception) {
                // Try next service
            }
        }
        return null
    }
}
