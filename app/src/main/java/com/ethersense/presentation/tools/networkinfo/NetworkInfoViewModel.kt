package com.ethersense.presentation.tools.networkinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethersense.data.model.NetworkInfo
import com.ethersense.data.source.NetworkInfoDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NetworkInfoUiState(
    val isLoading: Boolean = false,
    val networkInfo: NetworkInfo? = null,
    val error: String? = null,
    val isJapanese: Boolean = false
)

sealed class NetworkInfoEvent {
    data object Refresh : NetworkInfoEvent()
    data class SetLanguage(val isJapanese: Boolean) : NetworkInfoEvent()
}

@HiltViewModel
class NetworkInfoViewModel @Inject constructor(
    private val networkInfoDataSource: NetworkInfoDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(NetworkInfoUiState())
    val uiState: StateFlow<NetworkInfoUiState> = _uiState.asStateFlow()

    init {
        loadNetworkInfo()
    }

    fun onEvent(event: NetworkInfoEvent) {
        when (event) {
            is NetworkInfoEvent.Refresh -> loadNetworkInfo()
            is NetworkInfoEvent.SetLanguage -> {
                _uiState.value = _uiState.value.copy(isJapanese = event.isJapanese)
            }
        }
    }

    private fun loadNetworkInfo() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val info = networkInfoDataSource.getNetworkInfo()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    networkInfo = info
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
}
