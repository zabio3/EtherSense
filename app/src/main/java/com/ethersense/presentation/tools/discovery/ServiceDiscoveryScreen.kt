package com.ethersense.presentation.tools.discovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ethersense.data.model.DiscoveredService
import com.ethersense.data.model.ServiceProtocol

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDiscoveryScreen(
    onNavigateBack: () -> Unit,
    isJapanese: Boolean = false,
    viewModel: ServiceDiscoveryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(isJapanese) {
        viewModel.onEvent(ServiceDiscoveryEvent.SetLanguage(isJapanese))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isJapanese) "サービス検出" else "Service Discovery") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = if (isJapanese)
                    "ローカルネットワーク上のサービスとデバイスを検出します。"
                else
                    "Discover services and devices on your local network.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.selectedProtocol == DiscoveryProtocol.ALL,
                    onClick = { viewModel.onEvent(ServiceDiscoveryEvent.ProtocolChanged(DiscoveryProtocol.ALL)) },
                    label = { Text(if (isJapanese) "すべて" else "All") },
                    enabled = !uiState.isDiscovering
                )
                FilterChip(
                    selected = uiState.selectedProtocol == DiscoveryProtocol.MDNS,
                    onClick = { viewModel.onEvent(ServiceDiscoveryEvent.ProtocolChanged(DiscoveryProtocol.MDNS)) },
                    label = { Text("mDNS/Bonjour") },
                    enabled = !uiState.isDiscovering
                )
                FilterChip(
                    selected = uiState.selectedProtocol == DiscoveryProtocol.SSDP,
                    onClick = { viewModel.onEvent(ServiceDiscoveryEvent.ProtocolChanged(DiscoveryProtocol.SSDP)) },
                    label = { Text("UPnP/SSDP") },
                    enabled = !uiState.isDiscovering
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.isDiscovering) {
                    OutlinedButton(
                        onClick = { viewModel.onEvent(ServiceDiscoveryEvent.StopDiscovery) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isJapanese) "停止" else "Stop")
                    }
                } else {
                    Button(
                        onClick = { viewModel.onEvent(ServiceDiscoveryEvent.StartDiscovery) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isJapanese) "検出開始" else "Start Discovery")
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.onEvent(ServiceDiscoveryEvent.ClearResults) },
                    enabled = !uiState.isDiscovering && uiState.services.isNotEmpty()
                ) {
                    Text(if (isJapanese) "クリア" else "Clear")
                }
            }

            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isDiscovering) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                    Text(
                        text = if (isJapanese) "検出中..." else "Discovering...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (uiState.services.isNotEmpty()) {
                Text(
                    text = "${if (isJapanese) "検出されたサービス" else "Discovered Services"}: ${uiState.services.size}",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.services) { service ->
                    ServiceItem(service, isJapanese)
                }
            }
        }
    }
}

@Composable
private fun ServiceItem(service: DiscoveredService, isJapanese: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = when (service.protocol) {
                    ServiceProtocol.MDNS -> Icons.Default.Devices
                    ServiceProtocol.SSDP -> Icons.Default.Router
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = service.name,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = service.type,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                service.host?.let { host ->
                    Text(
                        text = buildString {
                            append(host)
                            service.port?.let { port -> append(":$port") }
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text = when (service.protocol) {
                        ServiceProtocol.MDNS -> "mDNS/Bonjour"
                        ServiceProtocol.SSDP -> "UPnP/SSDP"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                if (service.txtRecords.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            service.txtRecords.entries.take(3).forEach { (key, value) ->
                                Text(
                                    text = "$key: $value",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                            }
                            if (service.txtRecords.size > 3) {
                                Text(
                                    text = "+${service.txtRecords.size - 3} more",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
