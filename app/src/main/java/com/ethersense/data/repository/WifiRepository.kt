package com.ethersense.data.repository

import com.ethersense.data.model.WifiNetwork
import kotlinx.coroutines.flow.Flow

interface WifiRepository {
    fun observeNetworks(): Flow<List<WifiNetwork>>
    fun getConnectedNetwork(): WifiNetwork?
    fun triggerScan(): Boolean
    fun getLastScanTime(): Long
    fun getScanThrottleRemainingMs(): Long
}
