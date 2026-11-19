package com.example.cyclingcheckin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.cyclingcheckin.domain.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFE7EEFF),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF12305F),
    secondary = AccentOrange,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFFFEDE5),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF7A2E0E),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    outlineVariant = LightOutline,
    error = DangerRed,
    onError = androidx.compose.ui.graphics.Color.White
)

private val DarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF6E9BFF),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF061533),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF1D335C),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFDCE6FF),
    secondary = androidx.compose.ui.graphics.Color(0xFFFF9A6B),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF3A1505),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF43220F),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFFFDCCB),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = androidx.compose.ui.graphics.Color(0xFFFF6B66),
    onError = androidx.compose.ui.graphics.Color(0xFF3A0000)
)

val LocalRideLogColors = staticCompositionLocalOf { LightRideLogColors }

object RideLogTheme {
    val colors: RideLogColors
        @Composable get() = LocalRideLogColors.current
}

@Composable
fun RideLogTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val extraColors = if (dark) DarkRideLogColors else LightRideLogColors
    CompositionLocalProvider(LocalRideLogColors provides extraColors) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = RideLogTypography,
            content = content
        )
    }
}
