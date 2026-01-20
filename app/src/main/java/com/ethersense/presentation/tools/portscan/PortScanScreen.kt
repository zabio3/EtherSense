package com.ethersense.presentation.tools.portscan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ethersense.data.model.PortScanResult
import com.ethersense.data.model.PortStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortScanScreen(
    onNavigateBack: () -> Unit,
    isJapanese: Boolean = false,
    viewModel: PortScanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isJapanese) {
        viewModel.onEvent(PortScanEvent.SetLanguage(isJapanese))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isJapanese) "ポートスキャナー" else "Port Scanner") },
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
            OutlinedTextField(
                value = uiState.host,
                onValueChange = { viewModel.onEvent(PortScanEvent.HostChanged(it)) },
                label = { Text(if (isJapanese) "ホスト / IP アドレス" else "Host / IP Address") },
                placeholder = { Text("192.168.1.1") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { keyboardController?.hide() }
                ),
                enabled = !uiState.isScanning
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.scanMode == ScanMode.COMMON,
                    onClick = { viewModel.onEvent(PortScanEvent.ScanModeChanged(ScanMode.COMMON)) },
                    label = { Text(if (isJapanese) "一般的なポート" else "Common Ports") },
                    enabled = !uiState.isScanning
                )
                FilterChip(
                    selected = uiState.scanMode == ScanMode.RANGE,
                    onClick = { viewModel.onEvent(PortScanEvent.ScanModeChanged(ScanMode.RANGE)) },
                    label = { Text(if (isJapanese) "範囲指定" else "Port Range") },
                    enabled = !uiState.isScanning
                )
            }

            if (uiState.scanMode == ScanMode.RANGE) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.startPort.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { port ->
                                viewModel.onEvent(PortScanEvent.StartPortChanged(port))
                            }
                        },
                        label = { Text(if (isJapanese) "開始" else "Start") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = !uiState.isScanning
                    )
                    OutlinedTextField(
                        value = uiState.endPort.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { port ->
                                viewModel.onEvent(PortScanEvent.EndPortChanged(port))
                            }
                        },
                        label = { Text(if (isJapanese) "終了" else "End") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = !uiState.isScanning
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.isScanning) {
                    OutlinedButton(
                        onClick = { viewModel.onEvent(PortScanEvent.StopScan) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isJapanese) "停止" else "Stop")
                    }
                } else {
                    Button(
                        onClick = { viewModel.onEvent(PortScanEvent.StartScan) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isJapanese) "スキャン開始" else "Start Scan")
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.onEvent(PortScanEvent.ClearResults) },
                    enabled = !uiState.isScanning && uiState.results.isNotEmpty()
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

            if (uiState.isScanning || uiState.results.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (uiState.isScanning) {
                            LinearProgressIndicator(
                                progress = { uiState.progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(
                                label = if (isJapanese) "開いている" else "Open",
                                value = uiState.openPorts.toString(),
                                color = MaterialTheme.colorScheme.primary
                            )
                            StatItem(
                                label = if (isJapanese) "閉じている" else "Closed",
                                value = uiState.closedPorts.toString(),
                                color = MaterialTheme.colorScheme.error
                            )
                            StatItem(
                                label = if (isJapanese) "フィルタ済み" else "Filtered",
                                value = uiState.filteredPorts.toString(),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(uiState.results.filter { it.status == PortStatus.OPEN }) { result ->
                    PortResultItem(result, isJapanese)
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PortResultItem(result: PortScanResult, isJapanese: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (result.status) {
                PortStatus.OPEN -> MaterialTheme.colorScheme.primaryContainer
                PortStatus.CLOSED -> MaterialTheme.colorScheme.surfaceVariant
                PortStatus.FILTERED -> MaterialTheme.colorScheme.tertiaryContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = result.port.toString(),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                result.serviceName?.let { service ->
                    Text(
                        text = service,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = when (result.status) {
                    PortStatus.OPEN -> if (isJapanese) "開いている" else "Open"
                    PortStatus.CLOSED -> if (isJapanese) "閉じている" else "Closed"
                    PortStatus.FILTERED -> if (isJapanese) "フィルタ済み" else "Filtered"
                },
                style = MaterialTheme.typography.labelMedium,
                color = when (result.status) {
                    PortStatus.OPEN -> MaterialTheme.colorScheme.primary
                    PortStatus.CLOSED -> MaterialTheme.colorScheme.error
                    PortStatus.FILTERED -> MaterialTheme.colorScheme.tertiary
                }
            )
        }
    }
}
