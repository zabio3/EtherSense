package com.ethersense.data.repository

import com.ethersense.data.model.WifiNetwork
import com.ethersense.data.source.WifiScannerDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiRepositoryImpl @Inject constructor(
    private val dataSource: WifiScannerDataSource
) : WifiRepository {

    override fun observeNetworks(): Flow<List<WifiNetwork>> {
        return dataSource.startScanning()
    }

    override fun getConnectedNetwork(): WifiNetwork? {
        return dataSource.getConnectedNetwork()
    }

    override fun triggerScan(): Boolean {
        return dataSource.triggerScan()
    }

    override fun getLastScanTime(): Long {
        return dataSource.getLastScanTime()
    }

    override fun getScanThrottleRemainingMs(): Long {
        return dataSource.getScanThrottleRemainingMs()
    }
}
