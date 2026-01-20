package com.ethersense.presentation.tools.dns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethersense.data.model.DnsLookupResult
import com.ethersense.data.model.DnsRecordType
import com.ethersense.domain.usecase.DnsLookupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DnsLookupUiState(
    val domain: String = "",
    val selectedRecordType: DnsRecordType = DnsRecordType.A,
    val isLoading: Boolean = false,
    val result: DnsLookupResult? = null,
    val error: String? = null,
    val isJapanese: Boolean = false
)

sealed class DnsLookupEvent {
    data class DomainChanged(val domain: String) : DnsLookupEvent()
    data class RecordTypeChanged(val type: DnsRecordType) : DnsLookupEvent()
    data object Lookup : DnsLookupEvent()
    data object LookupAll : DnsLookupEvent()
    data object ClearResults : DnsLookupEvent()
    data class SetLanguage(val isJapanese: Boolean) : DnsLookupEvent()
}

@HiltViewModel
class DnsLookupViewModel @Inject constructor(
    private val dnsLookupUseCase: DnsLookupUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DnsLookupUiState())
    val uiState: StateFlow<DnsLookupUiState> = _uiState.asStateFlow()

    fun onEvent(event: DnsLookupEvent) {
        when (event) {
            is DnsLookupEvent.DomainChanged -> {
                _uiState.value = _uiState.value.copy(domain = event.domain, error = null)
            }
            is DnsLookupEvent.RecordTypeChanged -> {
                _uiState.value = _uiState.value.copy(selectedRecordType = event.type)
            }
            is DnsLookupEvent.Lookup -> lookup()
            is DnsLookupEvent.LookupAll -> lookupAll()
            is DnsLookupEvent.ClearResults -> clearResults()
            is DnsLookupEvent.SetLanguage -> {
                _uiState.value = _uiState.value.copy(isJapanese = event.isJapanese)
            }
        }
    }

    private fun lookup() {
        val domain = _uiState.value.domain.trim()
        if (domain.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                error = if (_uiState.value.isJapanese) "ドメインを入力してください" else "Please enter a domain"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, result = null, error = null)

        viewModelScope.launch {
            val result = dnsLookupUseCase(domain, _uiState.value.selectedRecordType)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                result = result,
                error = if (!result.isSuccess) result.errorMessage else null
            )
        }
    }

    private fun lookupAll() {
        val domain = _uiState.value.domain.trim()
        if (domain.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                error = if (_uiState.value.isJapanese) "ドメインを入力してください" else "Please enter a domain"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, result = null, error = null)

        viewModelScope.launch {
            val result = dnsLookupUseCase.lookupAll(domain)
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
