package com.ethersense.presentation.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ethersense.presentation.dashboard.DashboardScreen
import com.ethersense.presentation.dashboard.DashboardViewModel
import com.ethersense.presentation.dashboard.DashboardEvent
import com.ethersense.presentation.settings.SettingsScreen
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

@Composable
fun EtherSenseApp() {
    val navController = rememberNavController()
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val uiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = "dashboard"
    ) {
        composable("dashboard") {
            DashboardScreen(
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                viewModel = dashboardViewModel
            )
        }

        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                audioEnabled = uiState.audioEnabled,
                hapticEnabled = uiState.hapticEnabled,
                onAudioToggle = { enabled ->
                    dashboardViewModel.onEvent(DashboardEvent.ToggleAudio(enabled))
                },
                onHapticToggle = { enabled ->
                    dashboardViewModel.onEvent(DashboardEvent.ToggleHaptic(enabled))
                }
            )
        }
    }
}
