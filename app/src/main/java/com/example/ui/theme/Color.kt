package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Geometric Balance Warm Terracotta & Neutral Palette
val GeoPrimary = Color(0xFF8F4C38)
val GeoPrimaryLight = Color(0xFFFFDBD1)
val GeoPrimaryDark = Color(0xFFFFB5A0)
val GeoPrimaryContainer = Color(0xFFFFDBD1)
val GeoOnPrimaryContainer = Color(0xFF3A0B02)
val GeoPrimaryDarkContainer = Color(0xFF723523)
val GeoOnPrimaryDarkContainer = Color(0xFFFFDBD1)

val GeoSecondary = Color(0xFF77574E)
val GeoSecondaryContainer = Color(0xFFFFDBD1)
val GeoOnSecondaryContainer = Color(0xFF2C150F)
val GeoSecondaryDark = Color(0xFFE7BDB2)
val GeoSecondaryDarkContainer = Color(0xFF5D3F37)

val GeoTertiary = Color(0xFF6C5D2F)
val GeoTertiaryContainer = Color(0xFFF5E1A7)
val GeoTertiaryDark = Color(0xFFD8C58D)

// Surfaces & Backgrounds - Geometric Balance
val GeoBgLight = Color(0xFFFDF8F8)
val GeoSurfaceLight = Color(0xFFFFFFFF)
val GeoSurfaceVariantLight = Color(0xFFF4DEDA)
val GeoOnBgLight = Color(0xFF1C1B1F)
val GeoOnSurfaceLight = Color(0xFF1C1B1F)
val GeoOnSurfaceVariantLight = Color(0xFF524341)
val GeoOutlineLight = Color(0xFF857370)
val GeoOutlineVariantLight = Color(0xFFD8C2BE)

val GeoBgDark = Color(0xFF1A1110)
val GeoSurfaceDark = Color(0xFF231918)
val GeoSurfaceVariantDark = Color(0xFF352B2A)
val GeoOnBgDark = Color(0xFFEDE0DE)
val GeoOnSurfaceDark = Color(0xFFEDE0DE)
val GeoOnSurfaceVariantDark = Color(0xFFD8C2BE)
val GeoOutlineDark = Color(0xFFA08C89)
val GeoOutlineVariantDark = Color(0xFF524341)

// Financial Semantic Accents (Harmonized with Geometric Balance)
val IncomeGreen = Color(0xFF2E7D32)
val IncomeGreenLight = Color(0xFFDCFCE7)
val ExpenseRed = Color(0xFFBA1A1A)
val ExpenseRedLight = Color(0xFFFFDAD6)
val BudgetWarning = Color(0xFFB45309)
val BudgetWarningLight = Color(0xFFFEF3C7)

// Backward compatible aliases
val Emerald50 = Color(0xFFFDF8F8)
val Emerald100 = GeoPrimaryLight
val Emerald200 = Color(0xFFFFB5A0)
val Emerald400 = GeoPrimaryDark
val Emerald500 = GeoPrimary
val Emerald600 = Color(0xFF783E2D)
val Emerald700 = Color(0xFF623123)
val Emerald900 = Color(0xFF3A0B02)

val Slate950 = GeoBgDark
val Slate900 = GeoSurfaceDark
val Slate850 = Color(0xFF2B201F)
val Slate800 = GeoSurfaceVariantDark
val Slate700 = GeoOutlineDark
val Slate600 = GeoOnSurfaceVariantLight
val Slate400 = GeoOutlineLight
val Slate300 = GeoOutlineVariantLight
val Slate200 = Color(0xFFEADBCE)
val Slate100 = GeoSurfaceVariantLight
val Slate50 = GeoBgLight

// Category Palette - Geometric Balanced Accents
val CategoryColors = listOf(
    Color(0xFF8F4C38), // Terracotta
    Color(0xFF2E7D32), // Forest Green
    Color(0xFF0284C7), // Sky Blue
    Color(0xFF7C3AED), // Warm Violet
    Color(0xFFD97706), // Amber
    Color(0xFF0D9488), // Teal
    Color(0xFFC026D3), // Magenta
    Color(0xFFEA580C), // Orange
    Color(0xFF4F46E5), // Indigo
    Color(0xFF65A30D), // Olive Green
    Color(0xFFBE123C), // Crimson
    Color(0xFF78716C)  // Warm Stone
)

// Theme Presets Data Model & Color Palettes
data class ThemePresetInfo(
    val id: String,
    val name: String,
    val description: String,
    val primary: Color,
    val primaryLight: Color,
    val primaryDark: Color,
    val secondary: Color,
    val secondaryDark: Color,
    val tertiary: Color,
    val tertiaryDark: Color,
    val surfaceLight: Color = Color(0xFFFFFFFF),
    val bgLight: Color = Color(0xFFFDF8F8),
    val surfaceDark: Color = Color(0xFF1E1E24),
    val bgDark: Color = Color(0xFF131316)
)

val AppThemePresets = listOf(
    ThemePresetInfo(
        id = "terracotta",
        name = "Terracotta Sunset",
        description = "Warm earthy clay & amber tones",
        primary = Color(0xFF8F4C38),
        primaryLight = Color(0xFFFFDBD1),
        primaryDark = Color(0xFFFFB5A0),
        secondary = Color(0xFF77574E),
        secondaryDark = Color(0xFFE7BDB2),
        tertiary = Color(0xFF6C5D2F),
        tertiaryDark = Color(0xFFD8C58D),
        surfaceLight = Color(0xFFFFFFFF),
        bgLight = Color(0xFFFDF8F8),
        surfaceDark = Color(0xFF231918),
        bgDark = Color(0xFF1A1110)
    ),
    ThemePresetInfo(
        id = "emerald",
        name = "Emerald Forest",
        description = "Prosperous deep emerald & mint sage",
        primary = Color(0xFF059669),
        primaryLight = Color(0xFFD1FAE5),
        primaryDark = Color(0xFF6EE7B7),
        secondary = Color(0xFF047857),
        secondaryDark = Color(0xFFA7F3D0),
        tertiary = Color(0xFF0D9488),
        tertiaryDark = Color(0xFF5EEAD4),
        surfaceLight = Color(0xFFFFFFFF),
        bgLight = Color(0xFFF4FBF7),
        surfaceDark = Color(0xFF11221B),
        bgDark = Color(0xFF0A1612)
    ),
    ThemePresetInfo(
        id = "sapphire",
        name = "Ocean Sapphire",
        description = "Royal azure blue & deep ocean cyan",
        primary = Color(0xFF0284C7),
        primaryLight = Color(0xFFE0F2FE),
        primaryDark = Color(0xFF7DD3FC),
        secondary = Color(0xFF0369A1),
        secondaryDark = Color(0xFFBAE6FD),
        tertiary = Color(0xFF2563EB),
        tertiaryDark = Color(0xFF93C5FD),
        surfaceLight = Color(0xFFFFFFFF),
        bgLight = Color(0xFFF0F9FF),
        surfaceDark = Color(0xFF101B2B),
        bgDark = Color(0xFF0A101D)
    ),
    ThemePresetInfo(
        id = "amethyst",
        name = "Amethyst Luxury",
        description = "Rich royal violet & cosmic lavender",
        primary = Color(0xFF7C3AED),
        primaryLight = Color(0xFFEDE9FE),
        primaryDark = Color(0xFFC4B5FD),
        secondary = Color(0xFF6D28D9),
        secondaryDark = Color(0xFFDDD6FE),
        tertiary = Color(0xFF9333EA),
        tertiaryDark = Color(0xFFE9D5FF),
        surfaceLight = Color(0xFFFFFFFF),
        bgLight = Color(0xFFFAF5FF),
        surfaceDark = Color(0xFF1B132B),
        bgDark = Color(0xFF120C1F)
    ),
    ThemePresetInfo(
        id = "coral",
        name = "Sunset Coral",
        description = "Vibrant warm coral & golden tangerine",
        primary = Color(0xFFEA580C),
        primaryLight = Color(0xFFFFEDD5),
        primaryDark = Color(0xFFFDBA74),
        secondary = Color(0xFFC2410C),
        secondaryDark = Color(0xFFFED7AA),
        tertiary = Color(0xFFD97706),
        tertiaryDark = Color(0xFFFCD34D),
        surfaceLight = Color(0xFFFFFFFF),
        bgLight = Color(0xFFFFF7ED),
        surfaceDark = Color(0xFF261810),
        bgDark = Color(0xFF1A0E08)
    ),
    ThemePresetInfo(
        id = "cyber",
        name = "Cyber Titanium",
        description = "Modern slate titanium & neon lime",
        primary = Color(0xFF65A30D),
        primaryLight = Color(0xFFECFCCB),
        primaryDark = Color(0xFFBEF264),
        secondary = Color(0xFF475569),
        secondaryDark = Color(0xFF94A3B8),
        tertiary = Color(0xFF0F766E),
        tertiaryDark = Color(0xFF2DD4BF),
        surfaceLight = Color(0xFFFFFFFF),
        bgLight = Color(0xFFF8FAFC),
        surfaceDark = Color(0xFF1E242B),
        bgDark = Color(0xFF11151A)
    )
)

