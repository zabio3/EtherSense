package com.ethersense.presentation.tools.portscan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethersense.data.model.CommonPorts
import com.ethersense.data.model.PortScanResult
import com.ethersense.data.model.PortStatus
import com.ethersense.domain.usecase.PortScannerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PortScanUiState(
    val host: String = "",
    val scanMode: ScanMode = ScanMode.COMMON,
    val startPort: Int = 1,
    val endPort: Int = 1024,
    val isScanning: Boolean = false,
    val results: List<PortScanResult> = emptyList(),
    val openPorts: Int = 0,
    val closedPorts: Int = 0,
    val filteredPorts: Int = 0,
    val progress: Float = 0f,
    val error: String? = null,
    val isJapanese: Boolean = false
)

enum class ScanMode {
    COMMON,
    RANGE
}

sealed class PortScanEvent {
    data class HostChanged(val host: String) : PortScanEvent()
    data class ScanModeChanged(val mode: ScanMode) : PortScanEvent()
    data class StartPortChanged(val port: Int) : PortScanEvent()
    data class EndPortChanged(val port: Int) : PortScanEvent()
    data object StartScan : PortScanEvent()
    data object StopScan : PortScanEvent()
    data object ClearResults : PortScanEvent()
    data class SetLanguage(val isJapanese: Boolean) : PortScanEvent()
}

@HiltViewModel
class PortScanViewModel @Inject constructor(
    private val portScannerUseCase: PortScannerUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PortScanUiState())
    val uiState: StateFlow<PortScanUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null

    fun onEvent(event: PortScanEvent) {
        when (event) {
            is PortScanEvent.HostChanged -> {
                _uiState.value = _uiState.value.copy(host = event.host, error = null)
            }
            is PortScanEvent.ScanModeChanged -> {
                _uiState.value = _uiState.value.copy(scanMode = event.mode)
            }
            is PortScanEvent.StartPortChanged -> {
                _uiState.value = _uiState.value.copy(startPort = event.port.coerceIn(1, 65535))
            }
            is PortScanEvent.EndPortChanged -> {
                _uiState.value = _uiState.value.copy(endPort = event.port.coerceIn(1, 65535))
            }
            is PortScanEvent.StartScan -> startScan()
            is PortScanEvent.StopScan -> stopScan()
            is PortScanEvent.ClearResults -> clearResults()
            is PortScanEvent.SetLanguage -> {
                _uiState.value = _uiState.value.copy(isJapanese = event.isJapanese)
            }
        }
    }

    private fun startScan() {
        val host = _uiState.value.host.trim()
        if (host.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                error = if (_uiState.value.isJapanese) "ホストを入力してください" else "Please enter a host"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isScanning = true,
            results = emptyList(),
            openPorts = 0,
            closedPorts = 0,
            filteredPorts = 0,
            progress = 0f,
            error = null
        )

        val results = mutableListOf<PortScanResult>()
        var openCount = 0
        var closedCount = 0
        var filteredCount = 0

        scanJob = viewModelScope.launch {
            val flow = when (_uiState.value.scanMode) {
                ScanMode.COMMON -> portScannerUseCase.scanCommonPorts(host)
                ScanMode.RANGE -> portScannerUseCase.scanRange(
                    host,
                    _uiState.value.startPort,
                    _uiState.value.endPort
                )
            }

            val totalPorts = when (_uiState.value.scanMode) {
                ScanMode.COMMON -> CommonPorts.COMMON_PORTS.size
                ScanMode.RANGE -> _uiState.value.endPort - _uiState.value.startPort + 1
            }

            flow
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message,
                        isScanning = false
                    )
                }
                .collect { result ->
                    results.add(result)

                    when (result.status) {
                        PortStatus.OPEN -> openCount++
                        PortStatus.CLOSED -> closedCount++
                        PortStatus.FILTERED -> filteredCount++
                    }

                    _uiState.value = _uiState.value.copy(
                        results = results.toList().sortedWith(
                            compareBy({ it.status != PortStatus.OPEN }, { it.port })
                        ),
                        openPorts = openCount,
                        closedPorts = closedCount,
                        filteredPorts = filteredCount,
                        progress = results.size.toFloat() / totalPorts
                    )
                }

            _uiState.value = _uiState.value.copy(isScanning = false)
        }
    }

    private fun stopScan() {
        scanJob?.cancel()
        _uiState.value = _uiState.value.copy(isScanning = false)
    }

    private fun clearResults() {
        _uiState.value = _uiState.value.copy(
            results = emptyList(),
            openPorts = 0,
            closedPorts = 0,
            filteredPorts = 0,
            progress = 0f,
            error = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
    }
}
