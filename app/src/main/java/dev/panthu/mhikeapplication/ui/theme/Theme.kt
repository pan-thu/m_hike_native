package dev.panthu.mhikeapplication.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryForest,
    secondary = AccentTrailOrange,
    tertiary = InfoSky,
    background = BackgroundDark,
    surface = SurfaceDark,
    error = DangerBerry,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryForest,
    secondary = AccentTrailOrange,
    tertiary = InfoSky,
    background = BackgroundLight,
    surface = SurfaceLight,
    error = DangerBerry,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onError = Color.White
)

@Composable
fun MHikeApplicationTheme(
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