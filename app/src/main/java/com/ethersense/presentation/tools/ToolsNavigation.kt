package com.ethersense.presentation.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class ToolScreen(
    val route: String,
    val titleEn: String,
    val titleJa: String,
    val icon: ImageVector
) {
    data object Ping : ToolScreen(
        route = "tools/ping",
        titleEn = "Ping",
        titleJa = "Ping",
        icon = Icons.Default.NetworkPing
    )

    data object DnsLookup : ToolScreen(
        route = "tools/dns",
        titleEn = "DNS Lookup",
        titleJa = "DNS ルックアップ",
        icon = Icons.Default.Dns
    )

    data object Whois : ToolScreen(
        route = "tools/whois",
        titleEn = "Whois",
        titleJa = "Whois",
        icon = Icons.Default.Policy
    )

    data object WakeOnLan : ToolScreen(
        route = "tools/wol",
        titleEn = "Wake on LAN",
        titleJa = "Wake on LAN",
        icon = Icons.Default.Power
    )

    data object PortScanner : ToolScreen(
        route = "tools/portscan",
        titleEn = "Port Scanner",
        titleJa = "ポートスキャナー",
        icon = Icons.Default.Router
    )

    data object LanScanner : ToolScreen(
        route = "tools/lanscan",
        titleEn = "LAN Scanner",
        titleJa = "LAN スキャナー",
        icon = Icons.Default.Lan
    )

    data object ServiceDiscovery : ToolScreen(
        route = "tools/discovery",
        titleEn = "Service Discovery",
        titleJa = "サービス検出",
        icon = Icons.Default.Search
    )

    data object NetworkInfo : ToolScreen(
        route = "tools/netinfo",
        titleEn = "Network Info",
        titleJa = "ネットワーク情報",
        icon = Icons.Default.Info
    )

    fun getTitle(isJapanese: Boolean): String = if (isJapanese) titleJa else titleEn

    companion object {
        val allTools = listOf(
            Ping,
            DnsLookup,
            Whois,
            WakeOnLan,
            PortScanner,
            LanScanner,
            ServiceDiscovery,
            NetworkInfo
        )
    }
}
