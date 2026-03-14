package com.amirswitch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Settings screen for device configuration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentDeviceId: String,
    onDeviceIdChanged: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var deviceId by remember { mutableStateOf(currentDeviceId) }
    var hasChanges by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Device ID configuration
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Router,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Device Connection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = deviceId,
                        onValueChange = {
                            deviceId = it
                            hasChanges = it != currentDeviceId
                        },
                        label = { Text("Device ID") },
                        supportingText = {
                            Text("Must match the DEVICE_ID in your ESP32 firmware")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (hasChanges) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                onDeviceIdChanged(deviceId)
                                hasChanges = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save & Reconnect")
                        }
                    }
                }
            }

            // Info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "How It Works",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val steps = listOf(
                        "1. The ESP32 connects to your WiFi and Firebase",
                        "2. This app sends ON/OFF commands via Firebase",
                        "3. The ESP32 receives commands and toggles the relay",
                        "4. Schedules are checked every 10 seconds on the ESP32",
                        "5. Works from anywhere — not just your home WiFi!"
                    )

                    steps.forEach { step ->
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // App version
            Text(
                text = "AmirSwitch v1.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
