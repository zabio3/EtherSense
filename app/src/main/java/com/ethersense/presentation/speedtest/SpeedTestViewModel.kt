package com.ethersense.presentation.speedtest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethersense.data.model.SpeedTestPhase
import com.ethersense.data.model.SpeedTestProgress
import com.ethersense.data.model.SpeedTestResult
import com.ethersense.data.repository.AppLanguage
import com.ethersense.data.repository.SettingsRepository
import com.ethersense.domain.usecase.RunSpeedTestUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SpeedTestViewModel @Inject constructor(
    private val runSpeedTestUseCase: RunSpeedTestUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpeedTestUiState())
    val uiState: StateFlow<SpeedTestUiState> = _uiState.asStateFlow()

    init {
        loadLanguageSetting()
    }

    private fun loadLanguageSetting() {
        viewModelScope.launch {
            settingsRepository.language.collect { language ->
                _uiState.update { it.copy(language = language) }
            }
        }
    }

    fun startSpeedTest() {
        if (_uiState.value.isRunning) return

        _uiState.update { it.copy(isRunning = true, error = null) }

        viewModelScope.launch {
            runSpeedTestUseCase.observeProgress()
                .catch { e ->
                    val errorMessage = if (_uiState.value.isJapanese) {
                        e.message ?: "スピードテストに失敗しました"
                    } else {
                        e.message ?: "Speed test failed"
                    }
                    _uiState.update {
                        it.copy(
                            isRunning = false,
                            error = errorMessage,
                            progress = SpeedTestProgress(SpeedTestPhase.ERROR)
                        )
                    }
                }
                .collect { progress ->
                    _uiState.update { it.copy(progress = progress) }

                    when (progress.phase) {
                        SpeedTestPhase.DOWNLOAD -> {
                            _uiState.update {
                                it.copy(currentDownloadSpeed = progress.currentSpeedMbps)
                            }
                        }
                        SpeedTestPhase.UPLOAD -> {
                            _uiState.update {
                                it.copy(currentUploadSpeed = progress.currentSpeedMbps)
                            }
                        }
                        SpeedTestPhase.COMPLETED -> {
                            runFullTest()
                        }
                        else -> {}
                    }
                }
        }
    }

    private fun runFullTest() {
        viewModelScope.launch {
            try {
                val result = runSpeedTestUseCase.runTest()
                _uiState.update {
                    it.copy(
                        isRunning = false,
                        result = result,
                        history = (it.history + result).takeLast(10),
                        showCompletionBanner = true
                    )
                }
            } catch (e: Exception) {
                val errorMessage = if (_uiState.value.isJapanese) {
                    e.message ?: "テストに失敗しました"
                } else {
                    e.message ?: "Speed test failed"
                }
                _uiState.update {
                    it.copy(
                        isRunning = false,
                        error = errorMessage
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun dismissCompletionBanner() {
        _uiState.update { it.copy(showCompletionBanner = false) }
    }
}

data class SpeedTestUiState(
    val isRunning: Boolean = false,
    val progress: SpeedTestProgress = SpeedTestProgress(SpeedTestPhase.IDLE),
    val currentDownloadSpeed: Float = 0f,
    val currentUploadSpeed: Float = 0f,
    val result: SpeedTestResult? = null,
    val history: List<SpeedTestResult> = emptyList(),
    val error: String? = null,
    val showCompletionBanner: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM
) {
    val isJapanese: Boolean get() = language == AppLanguage.JAPANESE
}
