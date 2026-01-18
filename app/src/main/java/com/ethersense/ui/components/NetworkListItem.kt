package com.ethersense.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SignalWifi4Bar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ethersense.data.model.FrequencyBand
import com.ethersense.data.model.SecurityType
import com.ethersense.data.model.WifiNetwork
import com.ethersense.data.model.WifiStandard
import com.ethersense.ui.theme.CyanPrimary
import com.ethersense.ui.theme.EtherSenseTheme
import com.ethersense.ui.theme.SignalExcellent
import com.ethersense.ui.theme.SignalFair
import com.ethersense.ui.theme.SignalGood
import com.ethersense.ui.theme.SignalPoor
import com.ethersense.ui.theme.SignalWeak
import com.ethersense.ui.theme.SurfaceElevated

@Composable
fun NetworkListItem(
    network: WifiNetwork,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val signalColor = getSignalColor(network.rssi)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (network.isConnected) {
                CyanPrimary.copy(alpha = 0.1f)
            } else {
                SurfaceElevated
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(signalColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (network.isConnected) {
                        Icons.Default.SignalWifi4Bar
                    } else {
                        Icons.Default.Wifi
                    },
                    contentDescription = null,
                    tint = signalColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = network.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (network.isConnected) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (network.securityType != SecurityType.OPEN) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Secured",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    if (network.isConnected) {
                        Text(
                            text = "Connected",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanPrimary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "${network.rssi} dBm",
                        style = MaterialTheme.typography.bodySmall,
                        color = signalColor
                    )

                    Text(
                        text = "Ch ${network.channel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = network.frequencyBand.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (network.wifiStandard != WifiStandard.UNKNOWN) {
                        Text(
                            text = network.wifiStandard.displayName.take(6),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            SignalStrengthBars(rssi = network.rssi)
        }
    }
}

@Composable
private fun SignalStrengthBars(rssi: Int) {
    val bars = when {
        rssi >= -50 -> 4
        rssi >= -60 -> 3
        rssi >= -70 -> 2
        else -> 1
    }
    val color = getSignalColor(rssi)

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 1..4) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((8 + i * 4).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (i <= bars) color else color.copy(alpha = 0.2f)
                    )
            )
        }
    }
}

private fun getSignalColor(rssi: Int): Color {
    return when {
        rssi >= -50 -> SignalExcellent
        rssi >= -60 -> SignalGood
        rssi >= -70 -> SignalFair
        rssi >= -80 -> SignalPoor
        else -> SignalWeak
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D1A)
@Composable
private fun NetworkListItemPreview() {
    EtherSenseTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NetworkListItem(
                network = WifiNetwork(
                    ssid = "Home Network",
                    bssid = "00:11:22:33:44:55",
                    rssi = -45,
                    frequency = 5180,
                    channel = 36,
                    channelWidth = 80,
                    capabilities = "[WPA2-PSK]",
                    wifiStandard = WifiStandard.WIFI_6,
                    timestamp = System.currentTimeMillis(),
                    isConnected = true
                ),
                onClick = {}
            )

            NetworkListItem(
                network = WifiNetwork(
                    ssid = "Neighbor WiFi",
                    bssid = "AA:BB:CC:DD:EE:FF",
                    rssi = -72,
                    frequency = 2437,
                    channel = 6,
                    channelWidth = 20,
                    capabilities = "[WPA-PSK]",
                    wifiStandard = WifiStandard.WIFI_4,
                    timestamp = System.currentTimeMillis(),
                    isConnected = false
                ),
                onClick = {}
            )
        }
    }
}
