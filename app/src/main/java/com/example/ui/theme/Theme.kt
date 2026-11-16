package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val IosLightColorScheme = lightColorScheme(
    primary = IosBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5F1FF),
    onPrimaryContainer = IosBlue,
    secondary = ObsidianLeatherPocketTop,
    onSecondary = Color.White,
    tertiary = IosGreen,
    background = IosBackground,
    onBackground = IosTextPrimary,
    surface = IosCardSurface,
    onSurface = IosTextPrimary,
    surfaceVariant = IosSubtleSurface,
    onSurfaceVariant = IosTextSecondary,
    outline = IosDivider
)

@Composable
fun DuoVaultTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = IosLightColorScheme,
        typography = Typography,
        content = content
    )
}
