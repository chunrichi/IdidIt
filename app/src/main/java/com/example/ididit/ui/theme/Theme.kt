package com.example.ididit.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Immutable
data class ExtendedColors(
    // Heatmap levels
    val heatmapLevel0: Color = HeatLevel0,
    val heatmapLevel1: Color = HeatLevel1,
    val heatmapLevel2: Color = HeatLevel2,
    val heatmapLevel3: Color = HeatLevel3,
    val heatmapLevel4: Color = HeatLevel4,
    // Accent colors
    val accent: Color = AccentDark,
    val accentCoral: Color = AccentCoral,
    val accentSage: Color = AccentSage,
    val accentBlue: Color = AccentBlue,
    val accentYellow: Color = AccentYellow
)

val LocalExtendedColors = staticCompositionLocalOf { ExtendedColors() }

// Light theme - minimal black/white/gray with subtle accents
private val LightColorScheme = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    primaryContainer = Cloud,
    onPrimaryContainer = Ink,
    secondary = Graphite,
    onSecondary = Paper,
    secondaryContainer = Mist,
    onSecondaryContainer = Ink,
    tertiary = Gray,
    onTertiary = Paper,
    tertiaryContainer = Mist,
    onTertiaryContainer = Graphite,
    background = Paper,
    onBackground = Ink,
    surface = Cloud,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = Graphite,
    outline = Mist,
    outlineVariant = Mist,
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

// Dark theme
private val DarkColorScheme = darkColorScheme(
    primary = InkLight,
    onPrimary = Ink,
    primaryContainer = Graphite,
    onPrimaryContainer = Paper,
    secondary = GraphiteLight,
    onSecondary = Ink,
    secondaryContainer = AccentDark,
    onSecondaryContainer = Paper,
    background = PaperDark,
    onBackground = InkLight,
    surface = SurfaceDark,
    onSurface = InkLight,
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = GraphiteLight,
    outline = Color(0xFF3A3A3A),
    outlineVariant = Color(0xFF2A2A2A)
)

@Composable
fun IdidItTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val extendedColors = ExtendedColors(
        heatmapLevel0 = if (darkTheme) HeatLevel0Dark else HeatLevel0,
        heatmapLevel1 = if (darkTheme) HeatLevel1Dark else HeatLevel1,
        heatmapLevel2 = if (darkTheme) HeatLevel2Dark else HeatLevel2,
        heatmapLevel3 = if (darkTheme) HeatLevel3Dark else HeatLevel3,
        heatmapLevel4 = if (darkTheme) HeatLevel4Dark else HeatLevel4
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        window.statusBarColor = colorScheme.background.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = EditorialTypography,
            content = content
        )
    }
}
