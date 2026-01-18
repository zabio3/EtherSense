package com.ethersense.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ethersense.data.model.SignalMetrics
import com.ethersense.ui.theme.AccentOrange
import com.ethersense.ui.theme.CyanPrimary
import com.ethersense.ui.theme.EtherSenseTheme
import com.ethersense.ui.theme.PurpleSecondary
import com.ethersense.ui.theme.SignalExcellent
import com.ethersense.ui.theme.SignalFair
import com.ethersense.ui.theme.SignalPoor
import com.ethersense.ui.theme.SignalWeak
import com.ethersense.ui.theme.SurfaceElevated

@Composable
fun MetricCard(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )

                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MetricsGrid(
    metrics: SignalMetrics,
    modifier: Modifier = Modifier
) {
    val qualityColor = when {
        metrics.signalQuality >= 0.7f -> SignalExcellent
        metrics.signalQuality >= 0.5f -> SignalFair
        metrics.signalQuality >= 0.3f -> SignalPoor
        else -> SignalWeak
    }

    val interferenceColor = when {
        metrics.interferenceScore >= 0.7f -> SignalWeak
        metrics.interferenceScore >= 0.5f -> AccentOrange
        metrics.interferenceScore >= 0.3f -> SignalFair
        else -> SignalExcellent
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Signal Quality",
                value = "${metrics.qualityPercentage}",
                unit = "%",
                icon = Icons.Default.Wifi,
                accentColor = qualityColor,
                subtitle = metrics.qualityLevel.displayName,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Link Speed",
                value = "${metrics.linkSpeed}",
                unit = "Mbps",
                icon = Icons.Default.Speed,
                accentColor = CyanPrimary,
                subtitle = "Theoretical",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Est. Throughput",
                value = metrics.throughputDisplay,
                unit = "Mbps",
                icon = Icons.Default.Speed,
                accentColor = PurpleSecondary,
                subtitle = "Real-world estimate",
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Interference",
                value = "${(metrics.interferenceScore * 100).toInt()}",
                unit = "%",
                icon = Icons.Default.Warning,
                accentColor = interferenceColor,
                subtitle = metrics.interferenceLevel.displayName,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D1A)
@Composable
private fun MetricsGridPreview() {
    EtherSenseTheme {
        MetricsGrid(
            metrics = SignalMetrics(
                rssi = -55,
                signalQuality = 0.75f,
                linkSpeed = 866,
                interferenceScore = 0.25f,
                estimatedThroughput = 421.8f,
                snr = 40f,
                channelUtilization = 0.3f
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
