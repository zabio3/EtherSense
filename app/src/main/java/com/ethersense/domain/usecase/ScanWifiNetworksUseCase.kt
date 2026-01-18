package com.ethersense.domain.usecase

import com.ethersense.data.model.WifiNetwork
import com.ethersense.data.repository.WifiRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ScanWifiNetworksUseCase @Inject constructor(
    private val repository: WifiRepository
) {
    operator fun invoke(): Flow<List<WifiNetwork>> {
        return repository.observeNetworks()
    }

    fun triggerScan(): Boolean {
        return repository.triggerScan()
    }

    fun getLastScanTime(): Long {
        return repository.getLastScanTime()
    }
}
