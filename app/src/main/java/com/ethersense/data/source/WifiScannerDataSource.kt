package com.ethersense.data.source

import com.ethersense.data.model.WifiNetwork
import kotlinx.coroutines.flow.Flow

interface WifiScannerDataSource {
    fun startScanning(): Flow<List<WifiNetwork>>
    fun stopScanning()
    fun getConnectedNetwork(): WifiNetwork?
    fun triggerScan(): Boolean
    fun getLastScanTime(): Long
    fun getScanThrottleRemainingMs(): Long
}
