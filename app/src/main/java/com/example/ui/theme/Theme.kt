package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

fun getPresetColorScheme(presetId: String, isDark: Boolean): ColorScheme {
    val preset = AppThemePresets.firstOrNull { it.id.equals(presetId, ignoreCase = true) }
        ?: AppThemePresets[0]

    return if (isDark) {
        darkColorScheme(
            primary = preset.primaryDark,
            onPrimary = preset.bgDark,
            primaryContainer = preset.primary.copy(alpha = 0.35f),
            onPrimaryContainer = preset.primaryLight,
            secondary = preset.secondaryDark,
            onSecondary = preset.bgDark,
            secondaryContainer = preset.secondary.copy(alpha = 0.3f),
            onSecondaryContainer = preset.secondaryDark,
            tertiary = preset.tertiaryDark,
            onTertiary = preset.bgDark,
            background = preset.bgDark,
            onBackground = Color(0xFFEDE0DE),
            surface = preset.surfaceDark,
            onSurface = Color(0xFFEDE0DE),
            surfaceVariant = Color(0xFF2C2524),
            onSurfaceVariant = Color(0xFFD8C2BE),
            outline = Color(0xFFA08C89),
            outlineVariant = Color(0xFF524341),
            error = ExpenseRed,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = preset.primary,
            onPrimary = Color.White,
            primaryContainer = preset.primaryLight,
            onPrimaryContainer = Color(0xFF2A0D05),
            secondary = preset.secondary,
            onSecondary = Color.White,
            secondaryContainer = preset.primaryLight.copy(alpha = 0.7f),
            onSecondaryContainer = Color(0xFF2C150F),
            tertiary = preset.tertiary,
            onTertiary = Color.White,
            background = preset.bgLight,
            onBackground = Color(0xFF1C1B1F),
            surface = preset.surfaceLight,
            onSurface = Color(0xFF1C1B1F),
            surfaceVariant = Color(0xFFF2ECE9),
            onSurfaceVariant = Color(0xFF524341),
            outline = Color(0xFF857370),
            outlineVariant = Color(0xFFD8C2BE),
            error = ExpenseRed,
            onError = Color.White
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themePreset: String = "terracotta",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = getPresetColorScheme(themePreset, darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
