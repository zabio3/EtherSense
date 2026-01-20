package com.ethersense.presentation.tools.whois

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethersense.data.model.WhoisResult
import com.ethersense.domain.usecase.WhoisUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WhoisUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val result: WhoisResult? = null,
    val error: String? = null,
    val isJapanese: Boolean = false
)

sealed class WhoisEvent {
    data class QueryChanged(val query: String) : WhoisEvent()
    data object Lookup : WhoisEvent()
    data object ClearResults : WhoisEvent()
    data class SetLanguage(val isJapanese: Boolean) : WhoisEvent()
}

@HiltViewModel
class WhoisViewModel @Inject constructor(
    private val whoisUseCase: WhoisUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WhoisUiState())
    val uiState: StateFlow<WhoisUiState> = _uiState.asStateFlow()

    fun onEvent(event: WhoisEvent) {
        when (event) {
            is WhoisEvent.QueryChanged -> {
                _uiState.value = _uiState.value.copy(query = event.query, error = null)
            }
            is WhoisEvent.Lookup -> lookup()
            is WhoisEvent.ClearResults -> clearResults()
            is WhoisEvent.SetLanguage -> {
                _uiState.value = _uiState.value.copy(isJapanese = event.isJapanese)
            }
        }
    }

    private fun lookup() {
        val query = _uiState.value.query.trim()
        if (query.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                error = if (_uiState.value.isJapanese) "ドメインまたはIPアドレスを入力してください" else "Please enter a domain or IP address"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, result = null, error = null)

        viewModelScope.launch {
            val result = whoisUseCase(query)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                result = result,
                error = if (!result.isSuccess) result.errorMessage else null
            )
        }
    }

    private fun clearResults() {
        _uiState.value = _uiState.value.copy(result = null, error = null)
    }
}
