package com.ethersense.presentation.tools.ping

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import com.ethersense.data.model.PingProgress
import com.ethersense.data.model.PingResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PingScreen(
    onNavigateBack: () -> Unit,
    isJapanese: Boolean = false,
    viewModel: PingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isJapanese) {
        viewModel.onEvent(PingEvent.SetLanguage(isJapanese))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ping") },
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
            // Host input
            OutlinedTextField(
                value = uiState.host,
                onValueChange = { viewModel.onEvent(PingEvent.HostChanged(it)) },
                label = { Text(if (isJapanese) "ホスト / IP アドレス" else "Host / IP Address") },
                placeholder = { Text("google.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        if (!uiState.isRunning) {
                            viewModel.onEvent(PingEvent.StartPing)
                        }
                    }
                ),
                enabled = !uiState.isRunning
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Ping count slider
            Text(
                text = if (isJapanese) "Ping 回数: ${uiState.count}" else "Ping Count: ${uiState.count}",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = uiState.count.toFloat(),
                onValueChange = { viewModel.onEvent(PingEvent.CountChanged(it.toInt())) },
                valueRange = 1f..10f,
                steps = 8,
                enabled = !uiState.isRunning,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.isRunning) {
                    OutlinedButton(
                        onClick = { viewModel.onEvent(PingEvent.StopPing) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isJapanese) "停止" else "Stop")
                    }
                } else {
                    Button(
                        onClick = { viewModel.onEvent(PingEvent.StartPing) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isJapanese) "開始" else "Start")
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.onEvent(PingEvent.ClearResults) },
                    enabled = !uiState.isRunning && (uiState.progressList.isNotEmpty() || uiState.result != null)
                ) {
                    Text(if (isJapanese) "クリア" else "Clear")
                }
            }

            // Error message
            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress indicator
            if (uiState.isRunning) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                    Text(
                        text = if (isJapanese) "Ping 実行中..." else "Pinging...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Results
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.progressList) { progress ->
                    PingProgressItem(progress, isJapanese)
                }

                uiState.result?.let { result ->
                    item {
                        PingResultCard(result, isJapanese)
                    }
                }
            }
        }
    }
}

@Composable
private fun PingProgressItem(progress: PingProgress, isJapanese: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isJapanese) "応答 #${progress.sequenceNumber}" else "Reply #${progress.sequenceNumber}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "TTL=${progress.ttl}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${String.format("%.1f", progress.rtt)} ms",
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    progress.rtt < 50 -> MaterialTheme.colorScheme.primary
                    progress.rtt < 100 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

@Composable
private fun PingResultCard(result: PingResult, isJapanese: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (result.isSuccess)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = if (isJapanese) "結果" else "Results",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (result.ipAddress.isNotEmpty()) {
                ResultRow(
                    label = if (isJapanese) "IP アドレス" else "IP Address",
                    value = result.ipAddress
                )
            }

            ResultRow(
                label = if (isJapanese) "送信パケット" else "Packets Sent",
                value = result.packetsTransmitted.toString()
            )

            ResultRow(
                label = if (isJapanese) "受信パケット" else "Packets Received",
                value = result.packetsReceived.toString()
            )

            ResultRow(
                label = if (isJapanese) "パケットロス" else "Packet Loss",
                value = "${result.packetLoss.toInt()}%"
            )

            if (result.isSuccess) {
                Spacer(modifier = Modifier.height(8.dp))

                ResultRow(
                    label = if (isJapanese) "最小 RTT" else "Min RTT",
                    value = "${String.format("%.2f", result.rttMin)} ms"
                )

                ResultRow(
                    label = if (isJapanese) "平均 RTT" else "Avg RTT",
                    value = "${String.format("%.2f", result.rttAvg)} ms"
                )

                ResultRow(
                    label = if (isJapanese) "最大 RTT" else "Max RTT",
                    value = "${String.format("%.2f", result.rttMax)} ms"
                )
            }

            result.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
