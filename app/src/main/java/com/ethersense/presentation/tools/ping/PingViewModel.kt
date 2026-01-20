package com.ethersense.presentation.tools.ping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethersense.data.model.PingProgress
import com.ethersense.data.model.PingResult
import com.ethersense.domain.usecase.PingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PingUiState(
    val host: String = "",
    val count: Int = 4,
    val isRunning: Boolean = false,
    val progressList: List<PingProgress> = emptyList(),
    val result: PingResult? = null,
    val error: String? = null,
    val isJapanese: Boolean = false
)

sealed class PingEvent {
    data class HostChanged(val host: String) : PingEvent()
    data class CountChanged(val count: Int) : PingEvent()
    data object StartPing : PingEvent()
    data object StopPing : PingEvent()
    data object ClearResults : PingEvent()
    data class SetLanguage(val isJapanese: Boolean) : PingEvent()
}

@HiltViewModel
class PingViewModel @Inject constructor(
    private val pingUseCase: PingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PingUiState())
    val uiState: StateFlow<PingUiState> = _uiState.asStateFlow()

    private var pingJob: Job? = null

    fun onEvent(event: PingEvent) {
        when (event) {
            is PingEvent.HostChanged -> {
                _uiState.value = _uiState.value.copy(host = event.host, error = null)
            }
            is PingEvent.CountChanged -> {
                _uiState.value = _uiState.value.copy(count = event.count)
            }
            is PingEvent.StartPing -> startPing()
            is PingEvent.StopPing -> stopPing()
            is PingEvent.ClearResults -> clearResults()
            is PingEvent.SetLanguage -> {
                _uiState.value = _uiState.value.copy(isJapanese = event.isJapanese)
            }
        }
    }

    private fun startPing() {
        val host = _uiState.value.host.trim()
        if (host.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                error = if (_uiState.value.isJapanese) "ホストを入力してください" else "Please enter a host"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isRunning = true,
            progressList = emptyList(),
            result = null,
            error = null
        )

        pingJob = viewModelScope.launch {
            val progressList = mutableListOf<PingProgress>()

            pingUseCase(host, _uiState.value.count)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message,
                        isRunning = false
                    )
                }
                .onCompletion {
                    // Get final result
                    val result = pingUseCase.executeAndGetResult(host, _uiState.value.count)
                    _uiState.value = _uiState.value.copy(
                        result = result,
                        isRunning = false
                    )
                }
                .collect { progress ->
                    progressList.add(progress)
                    _uiState.value = _uiState.value.copy(progressList = progressList.toList())
                }
        }
    }

    private fun stopPing() {
        pingJob?.cancel()
        _uiState.value = _uiState.value.copy(isRunning = false)
    }

    private fun clearResults() {
        _uiState.value = _uiState.value.copy(
            progressList = emptyList(),
            result = null,
            error = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        pingJob?.cancel()
    }
}
