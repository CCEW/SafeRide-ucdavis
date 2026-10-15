package edu.ucdavis.saferide.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = AggieBlue,
    onPrimary = Color.White,
    primaryContainer = AggieBlueSoft,
    onPrimaryContainer = AggieBlue,
    secondary = AggieGold,
    onSecondary = AggieBlue,
    secondaryContainer = GoldFill,
    onSecondaryContainer = AggieBlue,
    tertiary = ArrivedGreen,
    onTertiary = Color.White,
    background = Canvas,
    onBackground = InkPrimary,
    surface = Color.White,
    onSurface = InkPrimary,
    surfaceVariant = Canvas,
    onSurfaceVariant = InkSecondary,
)

private val DarkColorScheme = darkColorScheme(
    primary = AggieGold,
    onPrimary = AggieBlue,
    primaryContainer = AggieBlueLight,
    onPrimaryContainer = Color.White,
    secondary = AggieGold,
    onSecondary = AggieBlue,
    secondaryContainer = AggieBlueLight,
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFF6BD48C),
    onTertiary = Color(0xFF00210C),
    background = NightCanvas,
    onBackground = NightInk,
    surface = NightSurface,
    onSurface = NightInk,
    surfaceVariant = NightSurface,
    onSurfaceVariant = NightInkSecondary,
)

/**
 * UC Davis branding is the default. Dynamic (wallpaper-based) color is intentionally
 * not used so Aggie Blue and Gold always show.
 */
@Composable
fun SafeRideTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
