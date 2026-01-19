package com.ethersense.presentation.dashboard

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ethersense.data.model.SignalMetrics
import com.ethersense.data.model.WifiNetwork
import com.ethersense.data.model.WifiStandard
import com.ethersense.data.repository.AppLanguage
import com.ethersense.ui.canvas.GlowingOrbView
import com.ethersense.ui.canvas.WaveformVisualizer
import com.ethersense.ui.components.MetricsGrid
import com.ethersense.ui.components.NetworkListItem
import com.ethersense.ui.components.PermissionRequestScreen
import com.ethersense.ui.theme.CyanPrimary
import com.ethersense.ui.theme.EtherSenseTheme
import com.ethersense.ui.theme.SurfaceDark
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.delay

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val locationPermission = rememberPermissionState(
        permission = Manifest.permission.ACCESS_FINE_LOCATION
    ) { granted ->
        viewModel.setPermissionGranted(granted)
        if (!granted) {
            viewModel.setPermissionPermanentlyDenied(true)
        }
    }

    LaunchedEffect(locationPermission.status) {
        viewModel.setPermissionGranted(locationPermission.status.isGranted)
        if (!locationPermission.status.isGranted && !locationPermission.status.shouldShowRationale) {
            viewModel.setPermissionPermanentlyDenied(true)
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(DashboardEvent.DismissError)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "EtherSense",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.isScanning) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(CyanPrimary)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = SurfaceDark,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        floatingActionButton = {
            if (uiState.hasPermission) {
                var isRefreshing by remember { mutableStateOf(false) }
                val infiniteTransition = rememberInfiniteTransition(label = "refresh")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing)
                    ),
                    label = "rotation"
                )

                LaunchedEffect(isRefreshing) {
                    if (isRefreshing) {
                        delay(2000)
                        isRefreshing = false
                    }
                }

                FloatingActionButton(
                    onClick = {
                        isRefreshing = true
                        viewModel.onEvent(DashboardEvent.TriggerScan)
                    },
                    containerColor = CyanPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = if (uiState.isJapanese) "再スキャン" else "Rescan",
                        modifier = if (isRefreshing || uiState.isLoading) {
                            Modifier.rotate(rotation)
                        } else {
                            Modifier
                        }
                    )
                }
            }
        }
    ) { padding ->
        if (!uiState.hasPermission) {
            PermissionRequestScreen(
                onRequestPermission = { locationPermission.launchPermissionRequest() },
                onOpenSettings = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    context.startActivity(intent)
                },
                isPermanentlyDenied = uiState.isPermanentlyDenied,
                modifier = Modifier.padding(padding)
            )
        } else {
            DashboardContent(
                uiState = uiState,
                onNetworkClick = { network ->
                    viewModel.onEvent(DashboardEvent.SelectNetwork(network))
                },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onNetworkClick: (WifiNetwork) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AnimatedVisibility(
                visible = uiState.currentMetrics != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                uiState.currentMetrics?.let { metrics ->
                    SignalVisualizationSection(
                        metrics = metrics,
                        rssiHistory = uiState.rssiHistory,
                        networkName = uiState.connectedNetwork?.displayName ?: "Unknown"
                    )
                }
            }
        }

        if (uiState.isLoading && uiState.networks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = CyanPrimary)
                        Text(
                            text = if (uiState.isJapanese) "ネットワークをスキャン中..." else "Scanning for networks...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (uiState.currentMetrics != null) {
            item {
                MetricsGrid(metrics = uiState.currentMetrics, isJapanese = uiState.isJapanese)
            }
        }

        if (uiState.networks.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uiState.isJapanese) "周辺ネットワーク" else "Nearby Networks",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (uiState.isJapanese) "${uiState.networkCount} 件検出" else "${uiState.networkCount} found",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(
                items = uiState.networks,
                key = { it.bssid }
            ) { network ->
                NetworkListItem(
                    network = network,
                    onClick = { onNetworkClick(network) }
                )
            }
        }
    }
}

@Composable
private fun SignalVisualizationSection(
    metrics: SignalMetrics,
    rssiHistory: List<Int>,
    networkName: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = networkName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${metrics.rssi} dBm",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = CyanPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            GlowingOrbView(
                signalQuality = metrics.signalQuality,
                modifier = Modifier.size(180.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (rssiHistory.size >= 2) {
                WaveformVisualizer(
                    rssiHistory = rssiHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D1A)
@Composable
private fun DashboardContentPreview() {
    EtherSenseTheme {
        DashboardContent(
            uiState = DashboardUiState(
                hasPermission = true,
                isScanning = true,
                connectedNetwork = WifiNetwork(
                    ssid = "Home Network",
                    bssid = "00:11:22:33:44:55",
                    rssi = -55,
                    frequency = 5180,
                    channel = 36,
                    channelWidth = 80,
                    capabilities = "[WPA2-PSK]",
                    wifiStandard = WifiStandard.WIFI_6,
                    timestamp = System.currentTimeMillis(),
                    isConnected = true
                ),
                currentMetrics = SignalMetrics(
                    rssi = -55,
                    signalQuality = 0.75f,
                    linkSpeed = 866,
                    interferenceScore = 0.2f,
                    estimatedThroughput = 450f,
                    snr = 40f,
                    channelUtilization = 0.3f
                ),
                rssiHistory = listOf(-50, -52, -55, -53, -55, -58, -55),
                networks = listOf(
                    WifiNetwork(
                        ssid = "Home Network",
                        bssid = "00:11:22:33:44:55",
                        rssi = -55,
                        frequency = 5180,
                        channel = 36,
                        channelWidth = 80,
                        capabilities = "[WPA2-PSK]",
                        wifiStandard = WifiStandard.WIFI_6,
                        timestamp = System.currentTimeMillis(),
                        isConnected = true
                    )
                )
            ),
            onNetworkClick = {}
        )
    }
}
