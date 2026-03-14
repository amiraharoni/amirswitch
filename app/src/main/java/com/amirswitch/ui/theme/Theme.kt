package com.amirswitch.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Brand colors
val PrimaryBlue = Color(0xFF1976D2)
val PrimaryDarkBlue = Color(0xFF0D47A1)
val AccentGreen = Color(0xFF4CAF50)
val AccentRed = Color(0xFFF44336)
val SurfaceDark = Color(0xFF121212)
val OnlineGreen = Color(0xFF66BB6A)
val OfflineRed = Color(0xFFEF5350)

private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    secondary = AccentGreen,
    onSecondary = Color.White,
    error = AccentRed,
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1C1B1F),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF64B5F6),
    onPrimary = Color.Black,
    secondary = Color(0xFF81C784),
    onSecondary = Color.Black,
    error = Color(0xFFEF9A9A),
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    background = SurfaceDark,
    onBackground = Color.White,
)

@Composable
fun AmirSwitchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
