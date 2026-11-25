package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme =
    lightColorScheme(
        primary = FitBluePrimary,
        onPrimary = Color.White,
        primaryContainer = FitBlueContainer,
        onPrimaryContainer = FitOnBlueContainer,
        secondary = FitGreenSecondary,
        onSecondary = Color.White,
        secondaryContainer = FitGreenContainer,
        onSecondaryContainer = FitOnGreenContainer,
        background = FitBackground,
        onBackground = FitTextPrimary,
        surface = FitSurface,
        onSurface = FitTextPrimary,
        surfaceVariant = FitSurfaceVariant,
        onSurfaceVariant = FitTextSecondary
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = FitBluePrimary,
        onPrimary = Color.White,
        primaryContainer = FitBlueDark,
        secondary = FitGreenSecondary,
        onSecondary = Color.White,
        background = Color(0xFF0F172A),
        surface = Color(0xFF1E293B),
        onBackground = Color.White,
        onSurface = Color.White
    )

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our branded theme for consistent look
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
