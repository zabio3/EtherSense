package com.ethersense.presentation.speedtest

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ethersense.data.model.SpeedTestPhase
import com.ethersense.data.model.SpeedTestResult
import com.ethersense.ui.theme.CyanPrimary
import com.ethersense.ui.theme.SignalExcellent
import com.ethersense.ui.theme.SurfaceDark
import kotlinx.coroutines.delay

@Composable
fun SpeedTestScreen(
    viewModel: SpeedTestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.dismissError()
        }
    }

    // Auto-dismiss completion banner after 4 seconds
    LaunchedEffect(uiState.showCompletionBanner) {
        if (uiState.showCompletionBanner) {
            delay(4000)
            viewModel.dismissCompletionBanner()
        }
    }

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Completion Banner spacer (to not overlap with content)
            AnimatedVisibility(
                visible = uiState.showCompletionBanner,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Spacer(modifier = Modifier.height(48.dp))
            }

            // Speed Gauge
            SpeedGauge(
                downloadSpeed = uiState.currentDownloadSpeed,
                uploadSpeed = uiState.currentUploadSpeed,
                isRunning = uiState.isRunning,
                phase = uiState.progress.phase,
                progress = uiState.progress.progress,
                isJapanese = uiState.isJapanese
            )

            // Start Button
            Button(
                onClick = { viewModel.startSpeedTest() },
                enabled = !uiState.isRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (uiState.isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = getPhaseText(uiState.progress.phase, uiState.isJapanese),
                        style = MaterialTheme.typography.titleMedium
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isJapanese) "テスト開始" else "Start Test",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // Results
            AnimatedVisibility(
                visible = uiState.result != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                uiState.result?.let { result ->
                    SpeedTestResultCard(result = result, isJapanese = uiState.isJapanese)
                }
            }

            // Usage Recommendations
            AnimatedVisibility(
                visible = uiState.result != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                uiState.result?.let { result ->
                    UsageRecommendations(result = result, isJapanese = uiState.isJapanese)
                }
            }
        }

        // Completion Banner at top
        AnimatedVisibility(
            visible = uiState.showCompletionBanner,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            CompletionBanner(
                result = uiState.result,
                isJapanese = uiState.isJapanese,
                onDismiss = { viewModel.dismissCompletionBanner() }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = SurfaceDark,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CompletionBanner(
    result: SpeedTestResult?,
    isJapanese: Boolean,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = SignalExcellent.copy(alpha = 0.15f),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = SignalExcellent,
                modifier = Modifier.size(32.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isJapanese) "スピードテスト完了" else "Speed Test Complete",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SignalExcellent
                )
                result?.let {
                    Text(
                        text = if (isJapanese) {
                            "↓ %.1f Mbps  ↑ %.1f Mbps".format(it.downloadSpeedMbps, it.uploadSpeedMbps)
                        } else {
                            "↓ %.1f Mbps  ↑ %.1f Mbps".format(it.downloadSpeedMbps, it.uploadSpeedMbps)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedGauge(
    downloadSpeed: Float,
    uploadSpeed: Float,
    isRunning: Boolean,
    phase: SpeedTestPhase,
    progress: Float,
    isJapanese: Boolean = true
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (isRunning) progress else 0f,
        animationSpec = tween(300),
        label = "progress"
    )

    val displaySpeed = when (phase) {
        SpeedTestPhase.DOWNLOAD -> downloadSpeed
        SpeedTestPhase.UPLOAD -> uploadSpeed
        else -> maxOf(downloadSpeed, uploadSpeed)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background arc
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 20.dp.toPx()
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                    // Background arc
                    drawArc(
                        color = Color(0xFF1A1A2E),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Progress arc
                    if (isRunning) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(CyanPrimary, Color(0xFF00E676))
                            ),
                            startAngle = 135f,
                            sweepAngle = 270f * animatedProgress,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                // Center content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (displaySpeed > 0) "%.1f".format(displaySpeed) else "--",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = CyanPrimary
                    )
                    Text(
                        text = "Mbps",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isRunning) {
                        Text(
                            text = getPhaseText(phase, isJapanese),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedTestResultCard(result: SpeedTestResult, isJapanese: Boolean = true) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isJapanese) "測定結果" else "Test Results",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SpeedMetricItem(
                    icon = Icons.Default.ArrowDownward,
                    label = if (isJapanese) "ダウンロード" else "Download",
                    value = "%.1f".format(result.downloadSpeedMbps),
                    unit = "Mbps",
                    color = Color(0xFF00E676)
                )

                SpeedMetricItem(
                    icon = Icons.Default.ArrowUpward,
                    label = if (isJapanese) "アップロード" else "Upload",
                    value = "%.1f".format(result.uploadSpeedMbps),
                    unit = "Mbps",
                    color = Color(0xFF7C4DFF)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SpeedMetricItem(
                    icon = Icons.Default.NetworkPing,
                    label = "Ping",
                    value = "${result.pingMs}",
                    unit = "ms",
                    color = CyanPrimary
                )

                SpeedMetricItem(
                    icon = Icons.Default.Speed,
                    label = "Jitter",
                    value = "${result.jitterMs}",
                    unit = "ms",
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

@Composable
private fun SpeedMetricItem(
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UsageRecommendations(result: SpeedTestResult, isJapanese: Boolean = true) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isJapanese) "利用用途の目安" else "Usage Recommendations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            UsageRow(
                label = if (isJapanese) "動画ストリーミング" else "Video Streaming",
                value = getLocalizedCapability(result.streamingCapability, isJapanese),
                color = getCapabilityColor(result.streamingCapability)
            )

            UsageRow(
                label = if (isJapanese) "オンラインゲーム" else "Online Gaming",
                value = getLocalizedCapability(result.gamingCapability, isJapanese),
                color = getCapabilityColor(result.gamingCapability)
            )

            UsageRow(
                label = if (isJapanese) "ビデオ通話" else "Video Calls",
                value = getLocalizedCapability(result.videoCallCapability, isJapanese),
                color = getCapabilityColor(result.videoCallCapability)
            )
        }
    }
}

@Composable
private fun UsageRow(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = color.copy(alpha = 0.1f)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = color,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

private fun getPhaseText(phase: SpeedTestPhase, isJapanese: Boolean = true): String {
    return if (isJapanese) {
        when (phase) {
            SpeedTestPhase.IDLE -> "待機中"
            SpeedTestPhase.CONNECTING -> "接続中..."
            SpeedTestPhase.PING -> "Ping測定中..."
            SpeedTestPhase.DOWNLOAD -> "ダウンロード測定中..."
            SpeedTestPhase.UPLOAD -> "アップロード測定中..."
            SpeedTestPhase.COMPLETED -> "完了"
            SpeedTestPhase.ERROR -> "エラー"
        }
    } else {
        when (phase) {
            SpeedTestPhase.IDLE -> "Idle"
            SpeedTestPhase.CONNECTING -> "Connecting..."
            SpeedTestPhase.PING -> "Measuring Ping..."
            SpeedTestPhase.DOWNLOAD -> "Measuring Download..."
            SpeedTestPhase.UPLOAD -> "Measuring Upload..."
            SpeedTestPhase.COMPLETED -> "Complete"
            SpeedTestPhase.ERROR -> "Error"
        }
    }
}

private fun getLocalizedCapability(capability: String, isJapanese: Boolean): String {
    if (isJapanese) return capability
    return when (capability) {
        "4K" -> "4K"
        "HD 1080p" -> "HD 1080p"
        "HD 720p" -> "HD 720p"
        "SD" -> "SD"
        "低画質のみ" -> "Low quality only"
        "最適" -> "Optimal"
        "良好" -> "Good"
        "可能" -> "Fair"
        "不向き" -> "Poor"
        "快適" -> "Excellent"
        "困難" -> "Difficult"
        else -> capability
    }
}

private fun getCapabilityColor(capability: String): Color {
    return when (capability) {
        "4K", "最適", "快適", "Optimal", "Excellent" -> Color(0xFF00E676)
        "HD 1080p", "良好", "Good" -> Color(0xFF69F0AE)
        "HD 720p", "可能", "Fair" -> Color(0xFFFFEB3B)
        "SD", "不安定", "Unstable" -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
}
