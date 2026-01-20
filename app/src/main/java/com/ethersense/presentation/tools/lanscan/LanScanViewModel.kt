package com.ethersense.presentation.tools.lanscan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethersense.data.model.LanDevice
import com.ethersense.domain.usecase.LanScannerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LanScanUiState(
    val isScanning: Boolean = false,
    val devices: List<LanDevice> = emptyList(),
    val localIp: String? = null,
    val subnet: String? = null,
    val scannedCount: Int = 0,
    val totalCount: Int = 254,
    val error: String? = null,
    val isJapanese: Boolean = false
)

sealed class LanScanEvent {
    data object StartScan : LanScanEvent()
    data object StopScan : LanScanEvent()
    data object ClearResults : LanScanEvent()
    data class SetLanguage(val isJapanese: Boolean) : LanScanEvent()
}

@HiltViewModel
class LanScanViewModel @Inject constructor(
    private val lanScannerUseCase: LanScannerUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LanScanUiState())
    val uiState: StateFlow<LanScanUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null

    init {
        loadLocalNetworkInfo()
    }

    private fun loadLocalNetworkInfo() {
        val (localIp, subnet) = lanScannerUseCase.getLocalNetworkInfo()
        _uiState.value = _uiState.value.copy(localIp = localIp, subnet = subnet)
    }

    fun onEvent(event: LanScanEvent) {
        when (event) {
            is LanScanEvent.StartScan -> startScan()
            is LanScanEvent.StopScan -> stopScan()
            is LanScanEvent.ClearResults -> clearResults()
            is LanScanEvent.SetLanguage -> {
                _uiState.value = _uiState.value.copy(isJapanese = event.isJapanese)
            }
        }
    }

    private fun startScan() {
        if (_uiState.value.localIp == null) {
            _uiState.value = _uiState.value.copy(
                error = if (_uiState.value.isJapanese) "ネットワークに接続されていません" else "Not connected to network"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isScanning = true,
            devices = emptyList(),
            scannedCount = 0,
            error = null
        )

        val devices = mutableListOf<LanDevice>()

        scanJob = viewModelScope.launch {
            lanScannerUseCase.scan(
                onProgress = { progress ->
                    _uiState.value = _uiState.value.copy(
                        scannedCount = progress.scannedCount,
                        totalCount = progress.totalCount
                    )
                }
            )
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message,
                        isScanning = false
                    )
                }
                .collect { device ->
                    devices.add(device)
                    _uiState.value = _uiState.value.copy(
                        devices = devices.toList().sortedBy { it.ipAddress }
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
            devices = emptyList(),
            scannedCount = 0,
            error = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
    }
}
