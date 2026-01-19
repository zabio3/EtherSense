package com.ethersense.presentation.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ethersense.presentation.dashboard.DashboardEvent
import com.ethersense.presentation.dashboard.DashboardScreen
import com.ethersense.presentation.dashboard.DashboardViewModel
import com.ethersense.presentation.diagnostics.DiagnosticsScreen
import com.ethersense.presentation.settings.SettingsScreen
import com.ethersense.presentation.speedtest.SpeedTestScreen
import com.ethersense.ui.theme.CyanPrimary
import com.ethersense.ui.theme.EtherSenseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EtherSenseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EtherSenseApp()
                }
            }
        }
    }
}

sealed class Screen(
    val route: String,
    val titleEn: String,
    val titleJa: String,
    val icon: ImageVector
) {
    data object Dashboard : Screen("dashboard", "Wi-Fi", "Wi-Fi", Icons.Default.Wifi)
    data object SpeedTest : Screen("speedtest", "Speed", "速度", Icons.Default.Speed)
    data object Diagnostics : Screen("diagnostics", "Diagnose", "診断", Icons.Default.Analytics)
    data object Settings : Screen("settings", "Settings", "設定", Icons.Default.Settings)

    fun getTitle(isJapanese: Boolean): String = if (isJapanese) titleJa else titleEn
}

private val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.SpeedTest,
    Screen.Diagnostics,
    Screen.Settings
)

@Composable
fun EtherSenseApp() {
    val navController = rememberNavController()
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val uiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                bottomNavItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    val title = screen.getTitle(uiState.isJapanese)
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = title
                            )
                        },
                        label = { Text(title) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyanPrimary,
                            selectedTextColor = CyanPrimary,
                            indicatorColor = CyanPrimary.copy(alpha = 0.1f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        onNavigateToSettings = {
                            navController.navigate(Screen.Settings.route)
                        },
                        viewModel = dashboardViewModel
                    )
                }

                composable(Screen.SpeedTest.route) {
                    SpeedTestScreen()
                }

                composable(Screen.Diagnostics.route) {
                    DiagnosticsScreen()
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        hapticEnabled = uiState.hapticEnabled,
                        onHapticToggle = { enabled ->
                            dashboardViewModel.onEvent(DashboardEvent.ToggleHaptic(enabled))
                        },
                        currentLanguage = uiState.language,
                        onLanguageChange = { language ->
                            dashboardViewModel.onEvent(DashboardEvent.ChangeLanguage(language))
                        }
                    )
                }
            }
        }
    }
}
