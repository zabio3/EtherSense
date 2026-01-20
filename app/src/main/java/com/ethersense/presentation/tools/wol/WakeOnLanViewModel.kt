package com.ethersense.presentation.tools.wol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethersense.domain.usecase.WakeOnLanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WakeOnLanUiState(
    val macAddress: String = "",
    val broadcastAddress: String = "255.255.255.255",
    val isLoading: Boolean = false,
    val lastResult: WolResult? = null,
    val error: String? = null,
    val isJapanese: Boolean = false
)

data class WolResult(
    val success: Boolean,
    val macAddress: String,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class WakeOnLanEvent {
    data class MacAddressChanged(val mac: String) : WakeOnLanEvent()
    data class BroadcastAddressChanged(val address: String) : WakeOnLanEvent()
    data object SendWakePacket : WakeOnLanEvent()
    data object ClearResults : WakeOnLanEvent()
    data class SetLanguage(val isJapanese: Boolean) : WakeOnLanEvent()
}

@HiltViewModel
class WakeOnLanViewModel @Inject constructor(
    private val wakeOnLanUseCase: WakeOnLanUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WakeOnLanUiState())
    val uiState: StateFlow<WakeOnLanUiState> = _uiState.asStateFlow()

    fun onEvent(event: WakeOnLanEvent) {
        when (event) {
            is WakeOnLanEvent.MacAddressChanged -> {
                val formatted = wakeOnLanUseCase.formatMacAddress(event.mac)
                _uiState.value = _uiState.value.copy(macAddress = formatted, error = null)
            }
            is WakeOnLanEvent.BroadcastAddressChanged -> {
                _uiState.value = _uiState.value.copy(broadcastAddress = event.address)
            }
            is WakeOnLanEvent.SendWakePacket -> sendWakePacket()
            is WakeOnLanEvent.ClearResults -> clearResults()
            is WakeOnLanEvent.SetLanguage -> {
                _uiState.value = _uiState.value.copy(isJapanese = event.isJapanese)
            }
        }
    }

    private fun sendWakePacket() {
        val mac = _uiState.value.macAddress.trim()
        if (mac.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                error = if (_uiState.value.isJapanese) "MACアドレスを入力してください" else "Please enter a MAC address"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, lastResult = null, error = null)

        viewModelScope.launch {
            val result = wakeOnLanUseCase(mac, _uiState.value.broadcastAddress)

            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        lastResult = WolResult(success = true, macAddress = mac)
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        lastResult = WolResult(success = false, macAddress = mac),
                        error = e.message
                    )
                }
            )
        }
    }

    private fun clearResults() {
        _uiState.value = _uiState.value.copy(lastResult = null, error = null)
    }
}
