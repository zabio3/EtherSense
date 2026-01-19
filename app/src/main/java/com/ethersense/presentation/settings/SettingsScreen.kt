package com.ethersense.presentation.settings

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ethersense.data.repository.AppLanguage
import com.ethersense.ui.theme.CyanPrimary
import com.ethersense.ui.theme.EtherSenseTheme
import com.ethersense.ui.theme.SurfaceElevated

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    hapticEnabled: Boolean,
    onHapticToggle: (Boolean) -> Unit,
    currentLanguage: AppLanguage = AppLanguage.SYSTEM,
    onLanguageChange: (AppLanguage) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (currentLanguage == AppLanguage.JAPANESE) "設定" else "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Language Section
            Text(
                text = if (currentLanguage == AppLanguage.JAPANESE) "言語 / Language" else "Language / 言語",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LanguageSelectionCard(
                currentLanguage = currentLanguage,
                onLanguageChange = onLanguageChange
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (currentLanguage == AppLanguage.JAPANESE) "フィードバック" else "Feedback",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SettingsToggleCard(
                icon = Icons.Default.Vibration,
                title = if (currentLanguage == AppLanguage.JAPANESE) "触覚フィードバック" else "Haptic Feedback",
                description = if (currentLanguage == AppLanguage.JAPANESE)
                    "チャンネル干渉検出時の振動パターン。干渉が強いほど振動が激しくなります。"
                else
                    "Vibration patterns when channel interference is detected. Stronger interference triggers more intense vibrations.",
                isChecked = hapticEnabled,
                onCheckedChange = onHapticToggle
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (currentLanguage == AppLanguage.JAPANESE) "アプリについて" else "About",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AboutCard(isJapanese = currentLanguage == AppLanguage.JAPANESE)

            Spacer(modifier = Modifier.height(16.dp))

            TechnicalInfoCard(isJapanese = currentLanguage == AppLanguage.JAPANESE)
        }
    }
}

@Composable
private fun LanguageSelectionCard(
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Language / 言語",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentLanguage.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CyanPrimary
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                AppLanguage.entries.forEach { language ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = language.displayName,
                                color = if (language == currentLanguage) CyanPrimary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onLanguageChange(language)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleCard(
    icon: ImageVector,
    title: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CyanPrimary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CyanPrimary,
                    checkedTrackColor = CyanPrimary.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
private fun AboutCard(isJapanese: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = CyanPrimary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "EtherSense",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isJapanese) "バージョン 1.0.0" else "Version 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isJapanese)
                        "「第六感」センサリーフィードバック機能を備えたWi-Fi環境アナライザー。信号強度の視覚化、干渉検出、実効スループットの推定が可能です。"
                    else
                        "A Wi-Fi environment analyzer with \"Sixth Sense\" sensory feedback features. Visualize signal strength, detect interference, and estimate real-world throughput.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TechnicalInfoCard(isJapanese: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = if (isJapanese) "技術詳細" else "Technical Details",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isJapanese) {
                TechnicalInfoItem("信号品質", "RSSIから計算: -30dBm (100%) ～ -90dBm (0%)")
                TechnicalInfoItem("干渉スコア", "同一チャンネルおよび隣接チャンネルのAPに基づく")
                TechnicalInfoItem("スループット推定", "リンク速度 × 品質 × (1 - 干渉) × 0.65")
                TechnicalInfoItem("スキャン制限", "Androidは2分間に4回までに制限")
                TechnicalInfoItem("Jitter（ジッター）", "遅延のばらつき。低いほど安定した接続")
            } else {
                TechnicalInfoItem("Signal Quality", "Calculated from RSSI: -30dBm (100%) to -90dBm (0%)")
                TechnicalInfoItem("Interference Score", "Based on co-channel and adjacent channel APs")
                TechnicalInfoItem("Throughput Estimate", "LinkSpeed x Quality x (1 - Interference) x 0.65")
                TechnicalInfoItem("Scan Throttle", "Android limits to 4 scans per 2 minutes")
                TechnicalInfoItem("Jitter", "Variation in latency. Lower is better for stable connection")
            }
        }
    }
}

@Composable
private fun TechnicalInfoItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = CyanPrimary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D1A)
@Composable
private fun SettingsScreenPreview() {
    EtherSenseTheme {
        var hapticEnabled by remember { mutableStateOf(true) }

        SettingsScreen(
            onNavigateBack = {},
            hapticEnabled = hapticEnabled,
            onHapticToggle = { hapticEnabled = it }
        )
    }
}
