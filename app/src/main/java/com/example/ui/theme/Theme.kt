package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = White,
    secondary = PurpleAccent,
    onSecondary = White,
    background = SurfaceLight,
    onBackground = CharcoalText,
    surface = SurfaceLight,
    onSurface = CharcoalText,
    outline = MutedSlateBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = SoftShieldBlue,
    onPrimary = DarkTextBlue,
    secondary = SoftPurpleBg,
    onSecondary = DarkPurpleAccent,
    background = DarkTextBlue,
    onBackground = White,
    surface = DarkTextBlue,
    onSurface = White,
    outline = VioletBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
