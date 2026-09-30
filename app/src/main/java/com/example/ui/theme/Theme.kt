package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Normal Sky Blue Theme
val SkyBlueColorScheme = lightColorScheme(
    primary = SkyBluePrimary,
    onPrimary = Color.White,
    primaryContainer = SkyBlueContainer,
    onPrimaryContainer = SkyBlueDark,
    secondary = SkyBlueLight,
    onSecondary = Color.White,
    secondaryContainer = SkyBlueContainer,
    onSecondaryContainer = SkyBlueDark,
    tertiary = AlertSevereOrange,
    onTertiary = Color.White,
    background = SkyBlueSurface,
    onBackground = TextPrimaryDark,
    surface = SkyBlueCard,
    onSurface = TextPrimaryDark,
    surfaceVariant = SkyBlueContainer,
    onSurfaceVariant = TextSecondaryDark,
    outline = SkyBlueBorder,
    error = AlertExtremeRed,
    onError = Color.White
)

val SkyBlueDarkColorScheme = darkColorScheme(
    primary = SkyBlueLight,
    onPrimary = Color.Black,
    primaryContainer = SkyBlueDark,
    onPrimaryContainer = Color.White,
    secondary = SkyBlueLight,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color.White,
    tertiary = AlertSevereOrange,
    onTertiary = Color.Black,
    background = Color(0xFF0B192C),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF14243B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E3557),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF2C4A70),
    error = AlertExtremeRed,
    onError = Color.White
)

@Composable
fun StormRadarTheme(
    darkTheme: Boolean = false, // Default to bright normal sky blue aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SkyBlueDarkColorScheme else SkyBlueColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
