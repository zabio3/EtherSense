package com.ethersense.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethersense.data.model.WifiNetwork
import com.ethersense.domain.analyzer.WifiAnalyzerEngine
import com.ethersense.domain.usecase.ScanWifiNetworksUseCase
import com.ethersense.feedback.FeedbackOrchestrator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val scanWifiNetworksUseCase: ScanWifiNetworksUseCase,
    private val wifiAnalyzerEngine: WifiAnalyzerEngine,
    private val feedbackOrchestrator: FeedbackOrchestrator
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null
    private var sixthSenseJob: Job? = null
    private val rssiHistory = mutableListOf<Int>()

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.StartScanning -> startScanning()
            is DashboardEvent.StopScanning -> stopScanning()
            is DashboardEvent.TriggerScan -> triggerScan()
            is DashboardEvent.ToggleAudio -> toggleAudio(event.enabled)
            is DashboardEvent.ToggleHaptic -> toggleHaptic(event.enabled)
            is DashboardEvent.StartSixthSenseMode -> startSixthSenseMode()
            is DashboardEvent.StopSixthSenseMode -> stopSixthSenseMode()
            is DashboardEvent.SelectNetwork -> selectNetwork(event.network)
            is DashboardEvent.DismissError -> dismissError()
            is DashboardEvent.RequestPermission -> Unit
            is DashboardEvent.OpenSettings -> Unit
        }
    }

    fun setPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(hasPermission = granted) }
        if (granted) {
            startScanning()
        }
    }

    fun setPermissionPermanentlyDenied(denied: Boolean) {
        _uiState.update { it.copy(isPermanentlyDenied = denied) }
    }

    private fun startScanning() {
        if (scanJob?.isActive == true) return

        _uiState.update { it.copy(isScanning = true, isLoading = true) }

        scanJob = viewModelScope.launch {
            scanWifiNetworksUseCase()
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            error = e.message ?: "Failed to scan networks",
                            isLoading = false
                        )
                    }
                }
                .collect { networks ->
                    processNetworks(networks)
                }
        }
    }

    private fun stopScanning() {
        scanJob?.cancel()
        scanJob = null
        feedbackOrchestrator.stop()
        _uiState.update { it.copy(isScanning = false) }
    }

    private fun triggerScan() {
        val success = scanWifiNetworksUseCase.triggerScan()
        if (!success) {
            _uiState.update {
                it.copy(error = "Scan throttled. Please wait before scanning again.")
            }
        }
    }

    private fun processNetworks(networks: List<WifiNetwork>) {
        val sortedNetworks = networks.sortedByDescending { it.rssi }
        val connected = networks.find { it.isConnected }

        val metrics = connected?.let { network ->
            wifiAnalyzerEngine.analyze(network, networks)
        }

        connected?.let { network ->
            updateRssiHistory(network.rssi)
        }

        _uiState.update {
            it.copy(
                networks = sortedNetworks,
                connectedNetwork = connected,
                currentMetrics = metrics,
                rssiHistory = rssiHistory.toList(),
                isLoading = false
            )
        }

        metrics?.let { m ->
            if (_uiState.value.audioEnabled || _uiState.value.hapticEnabled) {
                feedbackOrchestrator.provideFeedback(m)
            }
        }
    }

    private fun updateRssiHistory(rssi: Int) {
        rssiHistory.add(rssi)
        if (rssiHistory.size > MAX_HISTORY_SIZE) {
            rssiHistory.removeAt(0)
        }
    }

    private fun toggleAudio(enabled: Boolean) {
        _uiState.update { it.copy(audioEnabled = enabled) }
        feedbackOrchestrator.setAudioEnabled(enabled)
    }

    private fun toggleHaptic(enabled: Boolean) {
        _uiState.update { it.copy(hapticEnabled = enabled) }
        feedbackOrchestrator.setHapticEnabled(enabled)
    }

    private fun startSixthSenseMode() {
        _uiState.update { it.copy(audioEnabled = true) }
        feedbackOrchestrator.setAudioEnabled(true)

        feedbackOrchestrator.startContinuousFeedback {
            _uiState.value.currentMetrics
        }
    }

    private fun stopSixthSenseMode() {
        feedbackOrchestrator.stop()
    }

    private fun selectNetwork(network: WifiNetwork) {
        val metrics = wifiAnalyzerEngine.analyze(network, _uiState.value.networks)
        _uiState.update { it.copy(currentMetrics = metrics) }
    }

    private fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        feedbackOrchestrator.release()
    }

    companion object {
        private const val MAX_HISTORY_SIZE = 50
    }
}
