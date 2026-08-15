package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(key: String): ThemeMode {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: SYSTEM
        }
    }
}

data class UserPreferences(
    val currencyCode: String = "USD",
    val currencySymbol: String = "$",
    val isDarkMode: Boolean = false,
    val isFollowSystemTheme: Boolean = true,
    val themeMode: String = "system", // "system", "light", "dark"
    val isAppLockEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val appLockPin: String = "",
    val language: String = "en",
    val isBudgetAlertsEnabled: Boolean = true,
    val budgetAlertThresholdPercent: Int = 80, // Default 80%
    val isBackupReminderEnabled: Boolean = false,
    val backupReminderFrequency: String = "weekly", // "daily", "weekly", "biweekly", "monthly"
    val lastExportTimestamp: Long = 0L
)

class PreferenceRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("expense_tracker_prefs", Context.MODE_PRIVATE)

    private val _userPreferences = MutableStateFlow(loadPreferences())
    val userPreferences: StateFlow<UserPreferences> = _userPreferences.asStateFlow()

    private fun loadPreferences(): UserPreferences {
        val themeModeStr = prefs.getString("theme_mode", "system") ?: "system"
        val isFollowSystem = when (themeModeStr) {
            "light", "dark" -> false
            else -> prefs.getBoolean("follow_system_theme", true)
        }
        val isDark = when (themeModeStr) {
            "dark" -> true
            "light" -> false
            else -> prefs.getBoolean("is_dark_mode", false)
        }

        return UserPreferences(
            currencyCode = prefs.getString("currency_code", "USD") ?: "USD",
            currencySymbol = prefs.getString("currency_symbol", "$") ?: "$",
            isDarkMode = isDark,
            isFollowSystemTheme = isFollowSystem,
            themeMode = themeModeStr,
            isAppLockEnabled = prefs.getBoolean("is_app_lock_enabled", false),
            isBiometricEnabled = prefs.getBoolean("is_biometric_enabled", false),
            appLockPin = prefs.getString("app_lock_pin", "") ?: "",
            language = prefs.getString("app_language", "en") ?: "en",
            isBudgetAlertsEnabled = prefs.getBoolean("is_budget_alerts_enabled", true),
            budgetAlertThresholdPercent = prefs.getInt("budget_alert_threshold_percent", 80),
            isBackupReminderEnabled = prefs.getBoolean("is_backup_reminder_enabled", false),
            backupReminderFrequency = prefs.getString("backup_reminder_frequency", "weekly") ?: "weekly",
            lastExportTimestamp = prefs.getLong("last_export_timestamp", 0L)
        )
    }

    fun setLanguage(language: String) {
        prefs.edit()
            .putString("app_language", language)
            .apply()
        _userPreferences.value = loadPreferences()
    }

    fun setCurrency(code: String, symbol: String) {
        prefs.edit()
            .putString("currency_code", code)
            .putString("currency_symbol", symbol)
            .apply()
        _userPreferences.value = loadPreferences()
    }

    fun setThemeMode(mode: ThemeMode) {
        val isDark = mode == ThemeMode.DARK
        val followSystem = mode == ThemeMode.SYSTEM

        prefs.edit()
            .putString("theme_mode", mode.key)
            .putBoolean("is_dark_mode", isDark)
            .putBoolean("follow_system_theme", followSystem)
            .apply()
        _userPreferences.value = loadPreferences()
    }

    fun setThemePreference(isDark: Boolean, followSystem: Boolean) {
        val modeKey = when {
            followSystem -> "system"
            isDark -> "dark"
            else -> "light"
        }
        prefs.edit()
            .putString("theme_mode", modeKey)
            .putBoolean("is_dark_mode", isDark)
            .putBoolean("follow_system_theme", followSystem)
            .apply()
        _userPreferences.value = loadPreferences()
    }

    fun setBudgetAlertsEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean("is_budget_alerts_enabled", enabled)
            .apply()
        _userPreferences.value = loadPreferences()
    }

    fun setBudgetAlertThreshold(percent: Int) {
        val clamped = percent.coerceIn(10, 200)
        prefs.edit()
            .putInt("budget_alert_threshold_percent", clamped)
            .apply()
        _userPreferences.value = loadPreferences()
    }

    fun setBackupReminderEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean("is_backup_reminder_enabled", enabled)
            .apply()
        _userPreferences.value = loadPreferences()
    }

    fun setBackupReminderFrequency(frequency: String) {
        val validFrequency = when (frequency) {
            "daily", "biweekly", "monthly" -> frequency
            else -> "weekly"
        }
        prefs.edit()
            .putString("backup_reminder_frequency", validFrequency)
            .apply()
        _userPreferences.value = loadPreferences()
    }

    fun recordExportDone(timestamp: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putLong("last_export_timestamp", timestamp)
            .apply()
        _userPreferences.value = loadPreferences()
    }

    fun setAppLock(enabled: Boolean, pin: String) {
        val editor = prefs.edit()
            .putBoolean("is_app_lock_enabled", enabled)
            .putString("app_lock_pin", pin)
        if (!enabled) {
            editor.putBoolean("is_biometric_enabled", false)
        }
        editor.apply()
        _userPreferences.value = loadPreferences()
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean("is_biometric_enabled", enabled)
            .apply()
        _userPreferences.value = loadPreferences()
    }

    fun hasCleanedSampleData(): Boolean {
        return prefs.getBoolean("has_cleaned_sample_data_v2", false)
    }

    fun markSampleDataCleaned() {
        prefs.edit().putBoolean("has_cleaned_sample_data_v2", true).apply()
    }
}
