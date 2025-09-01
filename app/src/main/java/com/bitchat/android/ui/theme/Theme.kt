package com.bitchat.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Echo-inspired color schemes (UI-only)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFDAE9FF),
    onPrimary = Color(0xFF0B2347),
    secondary = Color(0xFF9BC7FF),
    onSecondary = Color(0xFF0B2347),
    background = Color(0xFF0A0F1A),
    onBackground = Color(0xFFDAE9FF),
    surface = Color(0xFF0F1726),
    onSurface = Color(0xFFCCE1FF),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF250000)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0B2347),
    onPrimary = Color.White,
    secondary = Color(0xFF165BB9),
    onSecondary = Color.White,
    background = Color(0xFFF6F9FF),
    onBackground = Color(0xFF0B2347),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF183A73),
    error = Color(0xFFB00020),
    onError = Color.White
)

// Echo tokens: gradients and layout constants provided via CompositionLocal
data class EchoTokens(
    val appBackground: Brush,
    val chatBackground: Brush,
    val surfaceBackground: Brush,
    val headerBackground: Brush,
    val myBubbleGradient: Brush,
    val otherBubbleGradient: Brush,
    val bubbleWidthRatio: Float = 0.90f,
    val bubblePadding: Float = 12f
)

object EchoDefaults {
    fun dark() = EchoTokens(
        appBackground = Brush.verticalGradient(
            listOf(Color(0xFF0A0F1A), Color(0xFF0D1526))
        ),
        chatBackground = Brush.verticalGradient(
            listOf(Color(0xFF0E1628).copy(alpha = 0.92f), Color(0xFF0A0F1A).copy(alpha = 0.92f))
        ),
        surfaceBackground = Brush.verticalGradient(
            listOf(Color(0xFF0F1726), Color(0xFF0F1726))
        ),
        headerBackground = Brush.verticalGradient(
            listOf(Color(0xFF0C1322), Color(0xFF0A0F1A))
        ),
        myBubbleGradient = Brush.linearGradient(
            listOf(Color(0xFF2A6CF0), Color(0xFF5BA8FF))
        ),
        otherBubbleGradient = Brush.linearGradient(
            listOf(Color(0xFF7B61FF), Color(0xFFB892FF))
        )
    )

    fun light() = EchoTokens(
        appBackground = Brush.verticalGradient(
            listOf(Color(0xFFF6F9FF), Color(0xFFEFF6FF))
        ),
        chatBackground = Brush.verticalGradient(
            listOf(Color(0xFFEFF5FF).copy(alpha = 0.92f), Color(0xFFF6F9FF).copy(alpha = 0.92f))
        ),
        surfaceBackground = Brush.verticalGradient(
            listOf(Color.White, Color.White)
        ),
        headerBackground = Brush.verticalGradient(
            listOf(Color(0xFFE9F2FF), Color(0xFFF6F9FF))
        ),
        myBubbleGradient = Brush.linearGradient(
            listOf(Color(0xFF2A6CF0), Color(0xFF5BA8FF))
        ),
        otherBubbleGradient = Brush.linearGradient(
            listOf(Color(0xFF7B61FF), Color(0xFFB892FF))
        )
    )
}

@Composable
fun BitchatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val tokens = if (darkTheme) EchoDefaults.dark() else EchoDefaults.light()

    CompositionLocalProvider(LocalEcho provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// CompositionLocal accessor
val LocalEcho = staticCompositionLocalOf { EchoDefaults.light() }
