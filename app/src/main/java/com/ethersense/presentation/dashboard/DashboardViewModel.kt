package com.ethersense.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethersense.data.model.WifiNetwork
import com.ethersense.data.repository.AppLanguage
import com.ethersense.data.repository.SettingsRepository
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
    private val feedbackOrchestrator: FeedbackOrchestrator,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null
    private val rssiHistory = mutableListOf<Int>()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.hapticEnabled.collect { enabled ->
                _uiState.update { it.copy(hapticEnabled = enabled) }
                feedbackOrchestrator.setHapticEnabled(enabled)
            }
        }
        viewModelScope.launch {
            settingsRepository.language.collect { language ->
                _uiState.update { it.copy(language = language) }
            }
        }
    }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.StartScanning -> startScanning()
            is DashboardEvent.StopScanning -> stopScanning()
            is DashboardEvent.TriggerScan -> triggerScan()
            is DashboardEvent.ToggleHaptic -> toggleHaptic(event.enabled)
            is DashboardEvent.ChangeLanguage -> changeLanguage(event.language)
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
                    val errorMessage = if (_uiState.value.isJapanese) {
                        e.message ?: "ネットワークのスキャンに失敗しました"
                    } else {
                        e.message ?: "Failed to scan networks"
                    }
                    _uiState.update {
                        it.copy(
                            error = errorMessage,
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
            val errorMessage = if (_uiState.value.isJapanese) {
                "スキャンが制限されています。しばらくお待ちください。"
            } else {
                "Scan throttled. Please wait before scanning again."
            }
            _uiState.update {
                it.copy(error = errorMessage)
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
            if (_uiState.value.hapticEnabled) {
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

    private fun toggleHaptic(enabled: Boolean) {
        _uiState.update { it.copy(hapticEnabled = enabled) }
        feedbackOrchestrator.setHapticEnabled(enabled)
        viewModelScope.launch {
            settingsRepository.setHapticEnabled(enabled)
        }
    }

    private fun changeLanguage(language: AppLanguage) {
        _uiState.update { it.copy(language = language) }
        viewModelScope.launch {
            settingsRepository.setLanguage(language)
        }
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
