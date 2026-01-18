package com.ethersense.domain.usecase

import com.ethersense.data.model.WifiNetwork
import com.ethersense.data.repository.WifiRepository
import javax.inject.Inject

class GetConnectedNetworkUseCase @Inject constructor(
    private val repository: WifiRepository
) {
    operator fun invoke(): WifiNetwork? {
        return repository.getConnectedNetwork()
    }
}
