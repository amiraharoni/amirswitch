package com.amirswitch.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amirswitch.ui.theme.AccentGreen
import com.amirswitch.ui.theme.AccentRed
import com.amirswitch.ui.theme.OfflineRed
import com.amirswitch.ui.theme.OnlineGreen
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main device control screen.
 * Shows a large power toggle button, online status, and navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(
    isOn: Boolean,
    isOnline: Boolean,
    lastSeen: Long,
    isLoading: Boolean,
    onToggle: () -> Unit,
    onNavigateToSchedules: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AmirSwitch") },
                actions = {
                    IconButton(onClick = onNavigateToSchedules) {
                        Icon(Icons.Default.Schedule, contentDescription = "Schedules")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Online status indicator
            OnlineStatusBadge(isOnline = isOnline, lastSeen = lastSeen)

            Spacer(modifier = Modifier.height(48.dp))

            // Large power button
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(160.dp),
                    strokeWidth = 4.dp
                )
            } else {
                PowerButton(
                    isOn = isOn,
                    onToggle = onToggle
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // State text
            Text(
                text = if (isOn) "ON" else "OFF",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOn) AccentGreen else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = if (isOn) "Power is flowing" else "Power is cut off",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Quick schedule info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToSchedules() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Manage Schedules",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun PowerButton(
    isOn: Boolean,
    onToggle: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isOn) 1.1f else 1.0f,
        animationSpec = tween(300),
        label = "scale"
    )

    val buttonColor by animateColorAsState(
        targetValue = if (isOn) AccentGreen else Color(0xFFBDBDBD),
        animationSpec = tween(300),
        label = "color"
    )

    val glowColor by animateColorAsState(
        targetValue = if (isOn) AccentGreen.copy(alpha = 0.3f) else Color.Transparent,
        animationSpec = tween(300),
        label = "glow"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(180.dp)
            .scale(scale)
    ) {
        // Glow effect when ON
        Box(
            modifier = Modifier
                .size(180.dp)
                .shadow(
                    elevation = if (isOn) 24.dp else 4.dp,
                    shape = CircleShape,
                    ambientColor = glowColor,
                    spotColor = glowColor
                )
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(buttonColor, buttonColor.copy(alpha = 0.8f))
                    ),
                    shape = CircleShape
                )
                .clickable { onToggle() }
        )

        // Power icon
        Icon(
            imageVector = if (isOn) Icons.Default.Power else Icons.Default.PowerOff,
            contentDescription = if (isOn) "Turn Off" else "Turn On",
            modifier = Modifier.size(72.dp),
            tint = Color.White
        )
    }
}

@Composable
private fun OnlineStatusBadge(isOnline: Boolean, lastSeen: Long) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    color = if (isOnline) OnlineGreen else OfflineRed,
                    shape = CircleShape
                )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isOnline) "Device Online" else "Device Offline",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isOnline) OnlineGreen else OfflineRed
        )

        if (!isOnline && lastSeen > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            val dateFormat = SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())
            val lastSeenStr = dateFormat.format(Date(lastSeen * 1000))
            Text(
                text = "(Last seen: $lastSeenStr)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
