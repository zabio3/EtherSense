package com.ethersense.presentation.tools.networkinfo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.ethersense.data.model.NetworkInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkInfoScreen(
    onNavigateBack: () -> Unit,
    isJapanese: Boolean = false,
    viewModel: NetworkInfoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(isJapanese) {
        viewModel.onEvent(NetworkInfoEvent.SetLanguage(isJapanese))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isJapanese) "ネットワーク情報" else "Network Info") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onEvent(NetworkInfoEvent.Refresh) },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
                .verticalScroll(rememberScrollState())
        ) {
            if (uiState.isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            uiState.networkInfo?.let { info ->
                NetworkInfoContent(info, isJapanese)
            }
        }
    }
}

@Composable
private fun NetworkInfoContent(info: NetworkInfo, isJapanese: Boolean) {
    // Connection Status
    InfoSection(title = if (isJapanese) "接続状態" else "Connection Status") {
        InfoRow(
            label = if (isJapanese) "ネットワークタイプ" else "Network Type",
            value = info.networkType ?: "Unknown"
        )
        InfoRow(
            label = if (isJapanese) "Wi-Fi接続" else "Wi-Fi Connected",
            value = if (info.isWifiConnected) {
                if (isJapanese) "はい" else "Yes"
            } else {
                if (isJapanese) "いいえ" else "No"
            }
        )
        InfoRow(
            label = if (isJapanese) "モバイルデータ" else "Cellular",
            value = if (info.isCellularConnected) {
                if (isJapanese) "はい" else "Yes"
            } else {
                if (isJapanese) "いいえ" else "No"
            }
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // IP Addresses
    InfoSection(title = if (isJapanese) "IPアドレス" else "IP Addresses") {
        InfoRow(
            label = if (isJapanese) "ローカルIP" else "Local IP",
            value = info.localIpAddress ?: "N/A"
        )
        InfoRow(
            label = if (isJapanese) "外部IP" else "External IP",
            value = info.externalIpAddress ?: "N/A"
        )
        InfoRow(
            label = if (isJapanese) "サブネットマスク" else "Subnet Mask",
            value = info.subnetMask ?: "N/A"
        )
        InfoRow(
            label = if (isJapanese) "ゲートウェイ" else "Gateway",
            value = info.gateway ?: "N/A"
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // DNS Servers
    InfoSection(title = if (isJapanese) "DNSサーバー" else "DNS Servers") {
        if (info.dnsServers.isEmpty()) {
            Text(
                text = "N/A",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            info.dnsServers.forEachIndexed { index, dns ->
                InfoRow(
                    label = "${if (isJapanese) "DNS" else "DNS"} ${index + 1}",
                    value = dns
                )
            }
        }
    }

    if (info.isWifiConnected) {
        Spacer(modifier = Modifier.height(16.dp))

        // Wi-Fi Info
        InfoSection(title = if (isJapanese) "Wi-Fi情報" else "Wi-Fi Info") {
            InfoRow(
                label = "SSID",
                value = info.ssid ?: "N/A"
            )
            InfoRow(
                label = "BSSID",
                value = info.bssid ?: "N/A"
            )
            InfoRow(
                label = if (isJapanese) "リンク速度" else "Link Speed",
                value = info.linkSpeed?.let { "$it Mbps" } ?: "N/A"
            )
            InfoRow(
                label = if (isJapanese) "周波数" else "Frequency",
                value = info.frequency?.let {
                    val band = when {
                        it < 3000 -> "2.4 GHz"
                        it < 6000 -> "5 GHz"
                        else -> "6 GHz"
                    }
                    "$it MHz ($band)"
                } ?: "N/A"
            )
            InfoRow(
                label = if (isJapanese) "信号強度" else "Signal Strength",
                value = info.rssi?.let { "$it dBm" } ?: "N/A"
            )
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
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
