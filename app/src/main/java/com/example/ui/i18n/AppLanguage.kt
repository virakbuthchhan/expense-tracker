package com.example.ui.i18n

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flag: String
) {
    ENGLISH(
        code = "en",
        displayName = "English",
        nativeName = "English",
        flag = "🇺🇸"
    ),
    KHMER(
        code = "km",
        displayName = "Khmer",
        nativeName = "ភាសាខ្មែរ",
        flag = "🇰🇭"
    );

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
        }
    }
}
