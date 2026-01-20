package com.ethersense.presentation.tools.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethersense.data.model.DiscoveredService
import com.ethersense.data.model.ServiceProtocol
import com.ethersense.data.source.NsdServiceDiscovery
import com.ethersense.data.source.SsdpDiscovery
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServiceDiscoveryUiState(
    val isDiscovering: Boolean = false,
    val services: List<DiscoveredService> = emptyList(),
    val selectedProtocol: DiscoveryProtocol = DiscoveryProtocol.ALL,
    val error: String? = null,
    val isJapanese: Boolean = false
)

enum class DiscoveryProtocol {
    ALL,
    MDNS,
    SSDP
}

sealed class ServiceDiscoveryEvent {
    data class ProtocolChanged(val protocol: DiscoveryProtocol) : ServiceDiscoveryEvent()
    data object StartDiscovery : ServiceDiscoveryEvent()
    data object StopDiscovery : ServiceDiscoveryEvent()
    data object ClearResults : ServiceDiscoveryEvent()
    data class SetLanguage(val isJapanese: Boolean) : ServiceDiscoveryEvent()
}

@HiltViewModel
class ServiceDiscoveryViewModel @Inject constructor(
    private val nsdServiceDiscovery: NsdServiceDiscovery,
    private val ssdpDiscovery: SsdpDiscovery
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServiceDiscoveryUiState())
    val uiState: StateFlow<ServiceDiscoveryUiState> = _uiState.asStateFlow()

    private var discoveryJob: Job? = null

    fun onEvent(event: ServiceDiscoveryEvent) {
        when (event) {
            is ServiceDiscoveryEvent.ProtocolChanged -> {
                _uiState.value = _uiState.value.copy(selectedProtocol = event.protocol)
            }
            is ServiceDiscoveryEvent.StartDiscovery -> startDiscovery()
            is ServiceDiscoveryEvent.StopDiscovery -> stopDiscovery()
            is ServiceDiscoveryEvent.ClearResults -> clearResults()
            is ServiceDiscoveryEvent.SetLanguage -> {
                _uiState.value = _uiState.value.copy(isJapanese = event.isJapanese)
            }
        }
    }

    private fun startDiscovery() {
        _uiState.value = _uiState.value.copy(
            isDiscovering = true,
            services = emptyList(),
            error = null
        )

        val services = mutableListOf<DiscoveredService>()

        discoveryJob = viewModelScope.launch {
            when (_uiState.value.selectedProtocol) {
                DiscoveryProtocol.ALL -> {
                    var mdnsCompleted = false
                    var ssdpCompleted = false

                    fun checkCompletion() {
                        if (mdnsCompleted && ssdpCompleted) {
                            _uiState.value = _uiState.value.copy(isDiscovering = false)
                        }
                    }

                    // Run both discoveries
                    launch {
                        nsdServiceDiscovery.discoverAllCommonServices()
                            .catch { /* Ignore errors, continue with other protocol */ }
                            .onCompletion {
                                mdnsCompleted = true
                                checkCompletion()
                            }
                            .collect { service ->
                                if (!services.any { it.name == service.name && it.host == service.host }) {
                                    services.add(service)
                                    _uiState.value = _uiState.value.copy(
                                        services = services.toList().sortedBy { it.name }
                                    )
                                }
                            }
                    }

                    launch {
                        ssdpDiscovery.discover()
                            .catch { /* Ignore errors */ }
                            .onCompletion {
                                ssdpCompleted = true
                                checkCompletion()
                            }
                            .collect { service ->
                                if (!services.any { it.name == service.name && it.host == service.host }) {
                                    services.add(service)
                                    _uiState.value = _uiState.value.copy(
                                        services = services.toList().sortedBy { it.name }
                                    )
                                }
                            }
                    }
                }
                DiscoveryProtocol.MDNS -> {
                    nsdServiceDiscovery.discoverAllCommonServices()
                        .catch { e ->
                            _uiState.value = _uiState.value.copy(error = e.message)
                        }
                        .onCompletion {
                            _uiState.value = _uiState.value.copy(isDiscovering = false)
                        }
                        .collect { service ->
                            if (!services.any { it.name == service.name && it.host == service.host }) {
                                services.add(service)
                                _uiState.value = _uiState.value.copy(
                                    services = services.toList().sortedBy { it.name }
                                )
                            }
                        }
                }
                DiscoveryProtocol.SSDP -> {
                    ssdpDiscovery.discover()
                        .catch { e ->
                            _uiState.value = _uiState.value.copy(error = e.message)
                        }
                        .onCompletion {
                            _uiState.value = _uiState.value.copy(isDiscovering = false)
                        }
                        .collect { service ->
                            if (!services.any { it.name == service.name && it.host == service.host }) {
                                services.add(service)
                                _uiState.value = _uiState.value.copy(
                                    services = services.toList().sortedBy { it.name }
                                )
                            }
                        }
                }
            }
        }
    }

    private fun stopDiscovery() {
        discoveryJob?.cancel()
        _uiState.value = _uiState.value.copy(isDiscovering = false)
    }

    private fun clearResults() {
        _uiState.value = _uiState.value.copy(
            services = emptyList(),
            error = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        discoveryJob?.cancel()
    }
}
