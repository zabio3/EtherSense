package com.ethersense.presentation.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethersense.data.model.WifiNetwork
import com.ethersense.data.repository.AppLanguage
import com.ethersense.data.repository.SettingsRepository
import com.ethersense.domain.analyzer.DistanceEstimator
import com.ethersense.domain.analyzer.NetworkDiagnosticsAnalyzer
import com.ethersense.domain.model.NetworkDiagnostics
import com.ethersense.domain.usecase.ScanWifiNetworksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiagnosticsUiState(
    val isLoading: Boolean = false,
    val isAnalyzing: Boolean = false,
    val connectedNetwork: WifiNetwork? = null,
    val diagnostics: NetworkDiagnostics? = null,
    val rssiHistory: List<Int> = emptyList(),
    val environmentType: DistanceEstimator.EnvironmentType = DistanceEstimator.EnvironmentType.RESIDENTIAL,
    val isDetailedMode: Boolean = true,
    val isJapanese: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val scanWifiNetworksUseCase: ScanWifiNetworksUseCase,
    private val diagnosticsAnalyzer: NetworkDiagnosticsAnalyzer,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    private val maxHistorySize = 50
    private var analysisJob: Job? = null
    private var lastAnalysisTime = 0L
    private val minAnalysisIntervalMs = 2000L // Minimum 2 seconds between auto-analyses

    init {
        loadSettings()
        startScanning()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.language.collect { language ->
                _uiState.update { it.copy(isJapanese = language == AppLanguage.JAPANESE) }
            }
        }
    }

    private fun startScanning() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            scanWifiNetworksUseCase().collect { networks ->
                val connected = networks.find { it.isConnected }

                // Update RSSI history
                val newHistory = if (connected != null) {
                    val history = _uiState.value.rssiHistory.toMutableList()
                    history.add(connected.rssi)
                    if (history.size > maxHistorySize) {
                        history.removeAt(0)
                    }
                    history
                } else {
                    emptyList()
                }

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        connectedNetwork = connected,
                        rssiHistory = newHistory
                    )
                }

                // Auto-analyze when connected (with debounce)
                if (connected != null) {
                    val now = System.currentTimeMillis()
                    if (now - lastAnalysisTime >= minAnalysisIntervalMs) {
                        runDiagnostics()
                    }
                }
            }
        }
    }

    fun runDiagnostics() {
        val network = _uiState.value.connectedNetwork ?: return

        // Guard against concurrent executions
        if (_uiState.value.isAnalyzing) return

        // Cancel any pending analysis job
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true) }
            lastAnalysisTime = System.currentTimeMillis()

            try {
                val diagnostics = diagnosticsAnalyzer.analyze(
                    network = network,
                    rssiHistory = _uiState.value.rssiHistory,
                    environmentType = _uiState.value.environmentType
                )

                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        diagnostics = diagnostics,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        error = e.message ?: "Analysis failed"
                    )
                }
            }
        }
    }

    fun setEnvironmentType(type: DistanceEstimator.EnvironmentType) {
        _uiState.update { it.copy(environmentType = type) }
        runDiagnostics()
    }

    fun toggleDetailedMode() {
        _uiState.update { it.copy(isDetailedMode = !it.isDetailedMode) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}
