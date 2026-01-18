package com.ethersense.data.source

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import com.ethersense.data.model.ChannelInfo
import com.ethersense.data.model.WifiNetwork
import com.ethersense.data.model.WifiStandard
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiScannerDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wifiManager: WifiManager
) : WifiScannerDataSource {

    private var lastScanTime: Long = 0L
    private var isScanning = false

    override fun startScanning(): Flow<List<WifiNetwork>> = callbackFlow {
        val connectedBssid = getConnectedBssid()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val success = intent.getBooleanExtra(
                    WifiManager.EXTRA_RESULTS_UPDATED, false
                )
                if (success) {
                    lastScanTime = System.currentTimeMillis()
                }
                val networks = processScanResults(connectedBssid)
                trySend(networks)
            }
        }

        val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(receiver, intentFilter)
        isScanning = true

        val initialResults = processScanResults(connectedBssid)
        trySend(initialResults)

        awaitClose {
            isScanning = false
            context.unregisterReceiver(receiver)
        }
    }

    override fun stopScanning() {
        isScanning = false
    }

    override fun triggerScan(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastScanTime < SCAN_THROTTLE_MS) {
            return false
        }
        @Suppress("DEPRECATION")
        return wifiManager.startScan()
    }

    override fun getConnectedNetwork(): WifiNetwork? {
        val connectionInfo = wifiManager.connectionInfo ?: return null
        val bssid = connectionInfo.bssid ?: return null

        @Suppress("DEPRECATION")
        val ssid = connectionInfo.ssid?.removeSurrounding("\"") ?: ""

        val scanResults = wifiManager.scanResults
        val matchingResult = scanResults.find { it.BSSID == bssid }

        return matchingResult?.let { result ->
            mapScanResultToNetwork(result, isConnected = true)
        } ?: WifiNetwork(
            ssid = ssid,
            bssid = bssid,
            rssi = connectionInfo.rssi,
            frequency = connectionInfo.frequency,
            channel = ChannelInfo.frequencyToChannel(connectionInfo.frequency),
            channelWidth = 20,
            capabilities = "",
            wifiStandard = WifiStandard.UNKNOWN,
            timestamp = System.currentTimeMillis(),
            isConnected = true
        )
    }

    override fun getLastScanTime(): Long = lastScanTime

    override fun getScanThrottleRemainingMs(): Long {
        val elapsed = System.currentTimeMillis() - lastScanTime
        return (SCAN_THROTTLE_MS - elapsed).coerceAtLeast(0)
    }

    private fun getConnectedBssid(): String? {
        return wifiManager.connectionInfo?.bssid
    }

    private fun processScanResults(connectedBssid: String?): List<WifiNetwork> {
        val scanResults = wifiManager.scanResults ?: return emptyList()
        return scanResults.map { result ->
            mapScanResultToNetwork(
                result,
                isConnected = result.BSSID == connectedBssid
            )
        }
    }

    private fun mapScanResultToNetwork(
        result: ScanResult,
        isConnected: Boolean
    ): WifiNetwork {
        val channelWidth = getChannelWidth(result)
        val wifiStandard = getWifiStandard(result)

        return WifiNetwork(
            ssid = result.SSID ?: "",
            bssid = result.BSSID,
            rssi = result.level,
            frequency = result.frequency,
            channel = ChannelInfo.frequencyToChannel(result.frequency),
            channelWidth = channelWidth,
            capabilities = result.capabilities,
            wifiStandard = wifiStandard,
            timestamp = result.timestamp / 1000,
            isConnected = isConnected
        )
    }

    private fun getChannelWidth(result: ScanResult): Int {
        return when (result.channelWidth) {
            ScanResult.CHANNEL_WIDTH_20MHZ -> 20
            ScanResult.CHANNEL_WIDTH_40MHZ -> 40
            ScanResult.CHANNEL_WIDTH_80MHZ -> 80
            ScanResult.CHANNEL_WIDTH_160MHZ -> 160
            ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> 160
            else -> 20
        }
    }

    private fun getWifiStandard(result: ScanResult): WifiStandard {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            when (result.wifiStandard) {
                ScanResult.WIFI_STANDARD_11N -> WifiStandard.WIFI_4
                ScanResult.WIFI_STANDARD_11AC -> WifiStandard.WIFI_5
                ScanResult.WIFI_STANDARD_11AX -> {
                    if (result.frequency > 5925) WifiStandard.WIFI_6E
                    else WifiStandard.WIFI_6
                }
                else -> WifiStandard.UNKNOWN
            }
        } else {
            inferWifiStandard(result)
        }
    }

    private fun inferWifiStandard(result: ScanResult): WifiStandard {
        return when {
            result.channelWidth >= ScanResult.CHANNEL_WIDTH_80MHZ -> WifiStandard.WIFI_5
            result.frequency > 5000 -> WifiStandard.WIFI_5
            else -> WifiStandard.WIFI_4
        }
    }

    companion object {
        private const val SCAN_THROTTLE_MS = 30_000L
    }
}
