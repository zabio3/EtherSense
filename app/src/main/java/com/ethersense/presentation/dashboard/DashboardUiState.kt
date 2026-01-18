package com.ethersense.presentation.dashboard

import com.ethersense.data.model.SignalMetrics
import com.ethersense.data.model.WifiNetwork

data class DashboardUiState(
    val isLoading: Boolean = false,
    val isScanning: Boolean = false,
    val hasPermission: Boolean = false,
    val isPermanentlyDenied: Boolean = false,
    val connectedNetwork: WifiNetwork? = null,
    val networks: List<WifiNetwork> = emptyList(),
    val currentMetrics: SignalMetrics? = null,
    val rssiHistory: List<Int> = emptyList(),
    val audioEnabled: Boolean = false,
    val hapticEnabled: Boolean = true,
    val error: String? = null
) {
    val networkCount: Int get() = networks.size
    val hasConnectedNetwork: Boolean get() = connectedNetwork != null
}

sealed interface DashboardEvent {
    data object StartScanning : DashboardEvent
    data object StopScanning : DashboardEvent
    data object RequestPermission : DashboardEvent
    data object OpenSettings : DashboardEvent
    data object TriggerScan : DashboardEvent
    data class ToggleAudio(val enabled: Boolean) : DashboardEvent
    data class ToggleHaptic(val enabled: Boolean) : DashboardEvent
    data object StartSixthSenseMode : DashboardEvent
    data object StopSixthSenseMode : DashboardEvent
    data class SelectNetwork(val network: WifiNetwork) : DashboardEvent
    data object DismissError : DashboardEvent
}
