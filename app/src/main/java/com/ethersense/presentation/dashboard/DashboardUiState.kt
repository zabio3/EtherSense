package com.ethersense.presentation.dashboard

import com.ethersense.data.model.SignalMetrics
import com.ethersense.data.model.WifiNetwork
import com.ethersense.data.repository.AppLanguage

data class DashboardUiState(
    val isLoading: Boolean = false,
    val isScanning: Boolean = false,
    val hasPermission: Boolean = false,
    val isPermanentlyDenied: Boolean = false,
    val connectedNetwork: WifiNetwork? = null,
    val networks: List<WifiNetwork> = emptyList(),
    val currentMetrics: SignalMetrics? = null,
    val rssiHistory: List<Int> = emptyList(),
    val hapticEnabled: Boolean = true,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val error: String? = null
) {
    val networkCount: Int get() = networks.size
    val hasConnectedNetwork: Boolean get() = connectedNetwork != null
    val isJapanese: Boolean get() = language == AppLanguage.JAPANESE
}

sealed interface DashboardEvent {
    data object StartScanning : DashboardEvent
    data object StopScanning : DashboardEvent
    data object RequestPermission : DashboardEvent
    data object OpenSettings : DashboardEvent
    data object TriggerScan : DashboardEvent
    data class ToggleHaptic(val enabled: Boolean) : DashboardEvent
    data class ChangeLanguage(val language: AppLanguage) : DashboardEvent
    data class SelectNetwork(val network: WifiNetwork) : DashboardEvent
    data object DismissError : DashboardEvent
}
