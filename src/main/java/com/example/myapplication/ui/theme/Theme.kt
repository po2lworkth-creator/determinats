package com.example.myapplication.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Plastic,
    onPrimary = TextPrimary,
    secondary = Paper,
    onSecondary = TextPrimary,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = Plastic,
    onPrimary = TextPrimary,
    secondary = Paper,
    onSecondary = TextPrimary,
    tertiary = Glass,
    background = AppBackground,
    surface = Surface,
    onSurface = TextPrimary,
    onBackground = TextPrimary,
    outline = TextSecondary,
    surfaceVariant = Color(0xFFF0F3F2)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}