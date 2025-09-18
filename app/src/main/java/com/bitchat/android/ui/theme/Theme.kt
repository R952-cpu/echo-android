package com.bitchat.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EchoPalette.BrandBlue,
    onPrimary = Color.White,
    secondary = EchoPalette.IncomingPurple,
    onSecondary = Color.White,
    background = Color(0xFF0B0D12),
    onBackground = Color(0xFFE6ECF7),
    surface = Color(0xFF13151C),
    onSurface = Color(0xFFE0E6F3),
    surfaceVariant = Color(0xFF1C1F27),
    onSurfaceVariant = Color(0xFFAAB4C6),
    outline = Color(0xFF2B3140),
    error = EchoPalette.AccentRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = EchoPalette.BrandBlue,
    onPrimary = Color.White,
    secondary = EchoPalette.IncomingPurple,
    onSecondary = Color.White,
    background = Color(0xFFF6FAFF),
    onBackground = Color(0xFF1F3148),
    surface = Color.White,
    onSurface = Color(0xFF24354C),
    surfaceVariant = Color(0xFFE8EFFC),
    onSurfaceVariant = Color(0xFF42526B),
    outline = Color(0xFFCAD7EB),
    error = EchoPalette.AccentRed,
    onError = Color.White
)

@Composable
fun BitchatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
