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

private val DarkColorScheme = darkColorScheme(
    primary = GeoPrimaryDark,
    onPrimary = GeoBgDark,
    primaryContainer = GeoPrimaryDarkContainer,
    onPrimaryContainer = GeoOnPrimaryDarkContainer,
    secondary = GeoSecondaryDark,
    onSecondary = GeoBgDark,
    secondaryContainer = GeoSecondaryDarkContainer,
    onSecondaryContainer = GeoSecondaryDark,
    tertiary = GeoTertiaryDark,
    onTertiary = GeoBgDark,
    background = GeoBgDark,
    onBackground = GeoOnBgDark,
    surface = GeoSurfaceDark,
    onSurface = GeoOnSurfaceDark,
    surfaceVariant = GeoSurfaceVariantDark,
    onSurfaceVariant = GeoOnSurfaceVariantDark,
    outline = GeoOutlineDark,
    outlineVariant = GeoOutlineVariantDark,
    error = ExpenseRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = GeoPrimary,
    onPrimary = Color.White,
    primaryContainer = GeoPrimaryContainer,
    onPrimaryContainer = GeoOnPrimaryContainer,
    secondary = GeoSecondary,
    onSecondary = Color.White,
    secondaryContainer = GeoSecondaryContainer,
    onSecondaryContainer = GeoOnSecondaryContainer,
    tertiary = GeoTertiary,
    onTertiary = Color.White,
    background = GeoBgLight,
    onBackground = GeoOnBgLight,
    surface = GeoSurfaceLight,
    onSurface = GeoOnSurfaceLight,
    surfaceVariant = GeoSurfaceVariantLight,
    onSurfaceVariant = GeoOnSurfaceVariantLight,
    outline = GeoOutlineLight,
    outlineVariant = GeoOutlineVariantLight,
    error = ExpenseRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            // Option for dynamic if desired, but default to custom tailored fintech theme
            DarkColorScheme
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
