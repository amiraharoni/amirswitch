package com.amirswitch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.amirswitch.ui.screens.DeviceScreen
import com.amirswitch.ui.screens.SchedulesScreen
import com.amirswitch.ui.screens.SettingsScreen
import com.amirswitch.ui.theme.AmirSwitchTheme
import com.amirswitch.viewmodel.DeviceViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AmirSwitchTheme {
                AmirSwitchApp()
            }
        }
    }
}

@Composable
fun AmirSwitchApp(viewModel: DeviceViewModel = viewModel()) {
    val navController = rememberNavController()

    // Collect state from ViewModel
    val isOn by viewModel.deviceState.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val lastSeen by viewModel.lastSeen.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val schedules by viewModel.schedules.collectAsState()
    val error by viewModel.error.collectAsState()

    // Show error as snackbar if needed
    LaunchedEffect(error) {
        error?.let {
            // In a production app, show a Snackbar here
            viewModel.clearError()
        }
    }

    NavHost(navController = navController, startDestination = "device") {
        // Main device control screen
        composable("device") {
            DeviceScreen(
                isOn = isOn,
                isOnline = isOnline,
                lastSeen = lastSeen,
                isLoading = isLoading,
                onToggle = { viewModel.toggleDevice() },
                onNavigateToSchedules = { navController.navigate("schedules") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }

        // Schedules management screen
        composable("schedules") {
            SchedulesScreen(
                schedules = schedules,
                onToggleSchedule = { id, enabled ->
                    viewModel.toggleScheduleEnabled(id, enabled)
                },
                onDeleteSchedule = { id ->
                    viewModel.deleteSchedule(id)
                },
                onAddSchedule = { schedule ->
                    viewModel.addSchedule(schedule)
                },
                onUpdateSchedule = { schedule ->
                    viewModel.updateSchedule(schedule)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Settings screen
        composable("settings") {
            SettingsScreen(
                currentDeviceId = viewModel.getDeviceId(),
                onDeviceIdChanged = { id ->
                    viewModel.setDeviceId(id)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
