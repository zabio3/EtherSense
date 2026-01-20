package com.ethersense.presentation.tools.wol

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WakeOnLanScreen(
    onNavigateBack: () -> Unit,
    isJapanese: Boolean = false,
    viewModel: WakeOnLanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isJapanese) {
        viewModel.onEvent(WakeOnLanEvent.SetLanguage(isJapanese))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wake on LAN") },
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
                    "マジックパケットを送信してリモートデバイスを起動します。"
                else
                    "Send a magic packet to wake up a remote device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = uiState.macAddress,
                onValueChange = { viewModel.onEvent(WakeOnLanEvent.MacAddressChanged(it)) },
                label = { Text(if (isJapanese) "MAC アドレス" else "MAC Address") },
                placeholder = { Text("00:11:22:33:44:55") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Next
                ),
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.broadcastAddress,
                onValueChange = { viewModel.onEvent(WakeOnLanEvent.BroadcastAddressChanged(it)) },
                label = { Text(if (isJapanese) "ブロードキャストアドレス" else "Broadcast Address") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        if (!uiState.isLoading) {
                            viewModel.onEvent(WakeOnLanEvent.SendWakePacket)
                        }
                    }
                ),
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.onEvent(WakeOnLanEvent.SendWakePacket) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(if (isJapanese) "Wake パケットを送信" else "Send Wake Packet")
            }

            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            uiState.lastResult?.let { result ->
                WolResultCard(result, isJapanese)
            }

            Spacer(modifier = Modifier.weight(1f))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = if (isJapanese) "注意" else "Note",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isJapanese)
                            "Wake on LANを使用するには、対象デバイスのBIOS/UEFIとOSでWOLが有効になっている必要があります。また、デバイスは有線接続（Ethernet）である必要があります。"
                        else
                            "Wake on LAN requires WOL to be enabled in the target device's BIOS/UEFI and OS. The device must also be connected via Ethernet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun WolResultCard(result: WolResult, isJapanese: Boolean) {
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (result.success)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (result.success) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (result.success)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )

            Text(
                text = if (result.success) {
                    if (isJapanese) "パケットを送信しました" else "Packet Sent"
                } else {
                    if (isJapanese) "送信に失敗しました" else "Failed to Send"
                },
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = result.macAddress,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = dateFormat.format(Date(result.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
