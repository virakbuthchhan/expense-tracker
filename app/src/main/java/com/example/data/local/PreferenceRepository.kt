package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserPreferences(
    val currencyCode: String = "USD",
    val currencySymbol: String = "$",
    val isDarkMode: Boolean = false,
    val isFollowSystemTheme: Boolean = true,
    val isAppLockEnabled: Boolean = false,
    val appLockPin: String = ""
)

class PreferenceRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("expense_tracker_prefs", Context.MODE_PRIVATE)

    private val _userPreferences = MutableStateFlow(loadPreferences())
    val userPreferences: StateFlow<UserPreferences> = _userPreferences.asStateFlow()

    private fun loadPreferences(): UserPreferences {
        return UserPreferences(
            currencyCode = prefs.getString("currency_code", "USD") ?: "USD",
            currencySymbol = prefs.getString("currency_symbol", "$") ?: "$",
            isDarkMode = prefs.getBoolean("is_dark_mode", false),
            isFollowSystemTheme = prefs.getBoolean("follow_system_theme", true),
            isAppLockEnabled = prefs.getBoolean("is_app_lock_enabled", false),
            appLockPin = prefs.getString("app_lock_pin", "") ?: ""
        )
    }

    fun setCurrency(code: String, symbol: String) {
        prefs.edit()
            .putString("currency_code", code)
            .putString("currency_symbol", symbol)
            .apply()
        _userPreferences.value = loadPreferences()
    }

    fun setThemePreference(isDark: Boolean, followSystem: Boolean) {
        prefs.edit()
            .putBoolean("is_dark_mode", isDark)
            .putBoolean("follow_system_theme", followSystem)
            .apply()
        _userPreferences.value = loadPreferences()
    }

    fun setAppLock(enabled: Boolean, pin: String) {
        prefs.edit()
            .putBoolean("is_app_lock_enabled", enabled)
            .putString("app_lock_pin", pin)
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
