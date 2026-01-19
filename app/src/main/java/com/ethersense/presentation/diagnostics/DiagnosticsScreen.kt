package com.ethersense.presentation.diagnostics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ethersense.domain.analyzer.DistanceEstimator
import com.ethersense.domain.model.DiagnosticLevel
import com.ethersense.domain.model.NetworkDiagnostics
import com.ethersense.domain.model.StabilityLevel
import com.ethersense.domain.model.TrendDirection
import com.ethersense.ui.theme.CyanPrimary
import com.ethersense.ui.theme.SignalExcellent
import com.ethersense.ui.theme.SignalFair
import com.ethersense.ui.theme.SignalGood
import com.ethersense.ui.theme.SignalPoor
import com.ethersense.ui.theme.SignalWeak
import com.ethersense.ui.theme.SurfaceDark
import com.ethersense.ui.theme.SurfaceVariant

@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            DiagnosticsHeader(
                isJapanese = uiState.isJapanese,
                isDetailedMode = uiState.isDetailedMode,
                onToggleMode = viewModel::toggleDetailedMode
            )

            // Loading/No Connection State
            when {
                uiState.isLoading -> {
                    LoadingState(uiState.isJapanese)
                }
                uiState.connectedNetwork == null -> {
                    NoConnectionState(uiState.isJapanese)
                }
                uiState.diagnostics == null && uiState.isAnalyzing -> {
                    AnalyzingState(uiState.isJapanese)
                }
                uiState.diagnostics != null -> {
                    DiagnosticsContent(
                        diagnostics = uiState.diagnostics!!,
                        isDetailedMode = uiState.isDetailedMode,
                        isJapanese = uiState.isJapanese,
                        environmentType = uiState.environmentType,
                        onEnvironmentChange = viewModel::setEnvironmentType,
                        onRefresh = viewModel::runDiagnostics,
                        isAnalyzing = uiState.isAnalyzing
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsHeader(
    isJapanese: Boolean,
    isDetailedMode: Boolean,
    onToggleMode: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Analytics,
                contentDescription = null,
                tint = CyanPrimary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isJapanese) "ネットワーク診断" else "Network Diagnostics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isJapanese) "詳細" else "Detail",
                style = MaterialTheme.typography.bodySmall,
                color = if (isDetailedMode) CyanPrimary else Color.Gray
            )
            Switch(
                checked = isDetailedMode,
                onCheckedChange = { onToggleMode() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CyanPrimary,
                    checkedTrackColor = CyanPrimary.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
private fun LoadingState(isJapanese: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = CyanPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isJapanese) "ネットワークをスキャン中..." else "Scanning networks...",
            color = Color.Gray
        )
    }
}

@Composable
private fun NoConnectionState(isJapanese: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = SignalFair,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isJapanese) "Wi-Fiに接続されていません" else "Not connected to Wi-Fi",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = if (isJapanese)
                "診断を実行するにはWi-Fiに接続してください"
            else
                "Connect to a Wi-Fi network to run diagnostics",
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AnalyzingState(isJapanese: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = CyanPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isJapanese) "診断を実行中..." else "Running diagnostics...",
            color = Color.Gray
        )
    }
}

@Composable
private fun DiagnosticsContent(
    diagnostics: NetworkDiagnostics,
    isDetailedMode: Boolean,
    isJapanese: Boolean,
    environmentType: DistanceEstimator.EnvironmentType,
    onEnvironmentChange: (DistanceEstimator.EnvironmentType) -> Unit,
    onRefresh: () -> Unit,
    isAnalyzing: Boolean
) {
    // Overall Score Card
    OverallScoreCard(
        diagnostics = diagnostics,
        isJapanese = isJapanese
    )

    // Quick Summary (Simple Mode)
    AnimatedVisibility(
        visible = !isDetailedMode,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        SimpleSummaryCard(
            diagnostics = diagnostics,
            isJapanese = isJapanese
        )
    }

    // Detailed Cards
    AnimatedVisibility(
        visible = isDetailedMode,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Distance Estimation
            DistanceCard(
                diagnostics = diagnostics,
                isJapanese = isJapanese,
                environmentType = environmentType,
                onEnvironmentChange = onEnvironmentChange
            )

            // Throughput Prediction
            ThroughputCard(
                diagnostics = diagnostics,
                isJapanese = isJapanese
            )

            // Link Margin
            LinkMarginCard(
                diagnostics = diagnostics,
                isJapanese = isJapanese
            )

            // Signal Trend
            SignalTrendCard(
                diagnostics = diagnostics,
                isJapanese = isJapanese
            )
        }
    }

    // Recommendations
    RecommendationsCard(
        diagnostics = diagnostics,
        isJapanese = isJapanese
    )

    // Refresh Button
    Button(
        onClick = onRefresh,
        enabled = !isAnalyzing,
        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isAnalyzing) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            Icon(Icons.Default.Refresh, contentDescription = null)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (isJapanese) "再診断" else "Re-analyze")
    }
}

@Composable
private fun OverallScoreCard(
    diagnostics: NetworkDiagnostics,
    isJapanese: Boolean
) {
    val levelColor = when (diagnostics.overallLevel) {
        DiagnosticLevel.EXCELLENT -> SignalExcellent
        DiagnosticLevel.GOOD -> SignalGood
        DiagnosticLevel.FAIR -> SignalFair
        DiagnosticLevel.POOR -> SignalPoor
        DiagnosticLevel.CRITICAL -> SignalWeak
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = diagnostics.network.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Large Score
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(levelColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${diagnostics.overallPercentage}",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = levelColor
                    )
                    Text(
                        text = if (isJapanese)
                            diagnostics.overallLevel.displayNameJa
                        else
                            diagnostics.overallLevel.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = levelColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "${diagnostics.network.rssi} dBm • Ch ${diagnostics.network.channel} • ${diagnostics.network.wifiStandard.displayName}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun SimpleSummaryCard(
    diagnostics: NetworkDiagnostics,
    isJapanese: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryRow(
                icon = Icons.Default.LocationOn,
                label = if (isJapanese) "推定距離" else "Est. Distance",
                value = "${String.format("%.1f", diagnostics.distanceEstimate.estimatedMeters)}m",
                color = CyanPrimary
            )
            SummaryRow(
                icon = Icons.Default.Speed,
                label = if (isJapanese) "予測速度" else "Est. Speed",
                value = "${String.format("%.0f", diagnostics.throughputPrediction.estimatedReal)} Mbps",
                color = SignalGood
            )
            SummaryRow(
                icon = Icons.Default.SignalCellularAlt,
                label = if (isJapanese) "安定性" else "Stability",
                value = if (isJapanese)
                    diagnostics.linkMarginAnalysis.stability.displayNameJa
                else
                    diagnostics.linkMarginAnalysis.stability.displayName,
                color = when (diagnostics.linkMarginAnalysis.stability) {
                    StabilityLevel.EXCELLENT -> SignalExcellent
                    StabilityLevel.GOOD -> SignalGood
                    StabilityLevel.MARGINAL -> SignalFair
                    StabilityLevel.UNSTABLE -> SignalWeak
                }
            )
        }
    }
}

@Composable
private fun SummaryRow(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, color = Color.Gray)
        }
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun DistanceCard(
    diagnostics: NetworkDiagnostics,
    isJapanese: Boolean,
    environmentType: DistanceEstimator.EnvironmentType,
    onEnvironmentChange: (DistanceEstimator.EnvironmentType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    DiagnosticCard(
        title = if (isJapanese) "距離推定" else "Distance Estimation",
        subtitle = "ITU-R P.1238",
        icon = Icons.Default.LocationOn,
        iconColor = CyanPrimary
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (isJapanese) "推定距離" else "Estimated", color = Color.Gray)
                Text(
                    "${String.format("%.1f", diagnostics.distanceEstimate.estimatedMeters)}m",
                    fontWeight = FontWeight.Bold,
                    color = CyanPrimary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (isJapanese) "範囲" else "Range", color = Color.Gray)
                Text(
                    "${String.format("%.1f", diagnostics.distanceEstimate.minMeters)} - ${String.format("%.1f", diagnostics.distanceEstimate.maxMeters)}m",
                    color = Color.Gray
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (isJapanese) "信頼度" else "Confidence", color = Color.Gray)
                Text(
                    "${(diagnostics.distanceEstimate.confidence * 100).toInt()}%",
                    color = if (diagnostics.distanceEstimate.confidence > 0.7f) SignalGood else SignalFair
                )
            }

            // Environment selector
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isJapanese) "環境タイプ" else "Environment", color = Color.Gray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (isJapanese) environmentType.displayNameJa else environmentType.displayName,
                            color = CyanPrimary
                        )
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = CyanPrimary
                        )
                    }
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DistanceEstimator.EnvironmentType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = {
                                Text(if (isJapanese) type.displayNameJa else type.displayName)
                            },
                            onClick = {
                                onEnvironmentChange(type)
                                expanded = false
                            },
                            leadingIcon = {
                                if (type == environmentType) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = CyanPrimary)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThroughputCard(
    diagnostics: NetworkDiagnostics,
    isJapanese: Boolean
) {
    DiagnosticCard(
        title = if (isJapanese) "スループット予測" else "Throughput Prediction",
        subtitle = "Shannon + MCS",
        icon = Icons.Default.Speed,
        iconColor = SignalGood
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (isJapanese) "Shannon理論上限" else "Shannon Capacity", color = Color.Gray)
                Text(
                    "${String.format("%.0f", diagnostics.throughputPrediction.shannonCapacity)} Mbps",
                    color = Color.Gray
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (isJapanese) "MCSベース" else "MCS-based", color = Color.Gray)
                Text(
                    "${String.format("%.0f", diagnostics.throughputPrediction.mcsBasedThroughput)} Mbps",
                    color = Color.Gray
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (isJapanese) "実効予測" else "Real-world Est.", color = Color.Gray)
                Text(
                    "${String.format("%.0f", diagnostics.throughputPrediction.estimatedReal)} Mbps",
                    fontWeight = FontWeight.Bold,
                    color = SignalGood
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("MCS Index", color = Color.Gray)
                Text("${diagnostics.throughputPrediction.mcsIndex} (${diagnostics.throughputPrediction.modulation})")
            }
        }
    }
}

@Composable
private fun LinkMarginCard(
    diagnostics: NetworkDiagnostics,
    isJapanese: Boolean
) {
    val stabilityColor = when (diagnostics.linkMarginAnalysis.stability) {
        StabilityLevel.EXCELLENT -> SignalExcellent
        StabilityLevel.GOOD -> SignalGood
        StabilityLevel.MARGINAL -> SignalFair
        StabilityLevel.UNSTABLE -> SignalWeak
    }

    DiagnosticCard(
        title = if (isJapanese) "リンクマージン" else "Link Margin",
        subtitle = if (isJapanese) "接続安定性" else "Connection Stability",
        icon = Icons.Default.NetworkCheck,
        iconColor = stabilityColor
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (isJapanese) "マージン" else "Margin", color = Color.Gray)
                Text(
                    "${String.format("%.1f", diagnostics.linkMarginAnalysis.marginDb)} dB",
                    fontWeight = FontWeight.Bold,
                    color = stabilityColor
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (isJapanese) "安定性" else "Stability", color = Color.Gray)
                Text(
                    if (isJapanese)
                        diagnostics.linkMarginAnalysis.stability.displayNameJa
                    else
                        diagnostics.linkMarginAnalysis.stability.displayName,
                    color = stabilityColor
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (isJapanese) "フェードマージン" else "Fade Margin", color = Color.Gray)
                Text(
                    "${String.format("%.1f", diagnostics.linkMarginAnalysis.fadeMarginDb)} dB",
                    color = if (diagnostics.linkMarginAnalysis.fadeMarginDb >= 0) SignalGood else SignalWeak
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (isJapanese) "余裕" else "Headroom", color = Color.Gray)
                Text(
                    if (isJapanese)
                        diagnostics.linkMarginAnalysis.headroomJa
                    else
                        diagnostics.linkMarginAnalysis.headroom
                )
            }
        }
    }
}

@Composable
private fun SignalTrendCard(
    diagnostics: NetworkDiagnostics,
    isJapanese: Boolean
) {
    val trendIcon = when (diagnostics.signalTrend.direction) {
        TrendDirection.IMPROVING -> Icons.Default.TrendingUp
        TrendDirection.STABLE -> Icons.Default.TrendingFlat
        TrendDirection.DEGRADING -> Icons.Default.TrendingDown
    }

    val trendColor = when (diagnostics.signalTrend.direction) {
        TrendDirection.IMPROVING -> SignalExcellent
        TrendDirection.STABLE -> SignalGood
        TrendDirection.DEGRADING -> SignalWeak
    }

    DiagnosticCard(
        title = if (isJapanese) "信号トレンド" else "Signal Trend",
        subtitle = if (isJapanese) "時間変化分析" else "Time Analysis",
        icon = trendIcon,
        iconColor = trendColor
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (isJapanese) "傾向" else "Direction", color = Color.Gray)
                Text(
                    if (isJapanese)
                        diagnostics.signalTrend.direction.displayNameJa
                    else
                        diagnostics.signalTrend.direction.displayName,
                    fontWeight = FontWeight.Bold,
                    color = trendColor
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (isJapanese) "変化率" else "Rate", color = Color.Gray)
                Text(
                    "${String.format("%.2f", diagnostics.signalTrend.rateOfChange)} dB/s"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (isJapanese) "移動平均" else "Moving Avg", color = Color.Gray)
                Text("${String.format("%.1f", diagnostics.signalTrend.movingAverage)} dBm")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (isJapanese) "分散" else "Variance", color = Color.Gray)
                Text(
                    String.format("%.1f", diagnostics.signalTrend.variance),
                    color = if (diagnostics.signalTrend.variance < 10f) SignalGood else SignalFair
                )
            }

            // Quality prediction
            diagnostics.qualityPrediction.let { prediction ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(if (isJapanese) "5秒後予測" else "5s Prediction", color = Color.Gray)
                    Text(
                        "${prediction.predictedRssi} dBm (${(prediction.confidence * 100).toInt()}%)",
                        color = CyanPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendationsCard(
    diagnostics: NetworkDiagnostics,
    isJapanese: Boolean
) {
    val recommendations = if (isJapanese)
        diagnostics.recommendationsJa
    else
        diagnostics.recommendations

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isJapanese) "推奨事項" else "Recommendations",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            recommendations.forEach { recommendation ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("•", color = CyanPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = recommendation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(title, fontWeight = FontWeight.Bold)
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            content()
        }
    }
}
