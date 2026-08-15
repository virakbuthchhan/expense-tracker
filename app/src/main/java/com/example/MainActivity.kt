package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.service.BackupReminderService
import com.example.data.service.BudgetNotificationService
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.getAppStrings
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ExpenseViewModel

class MainActivity : FragmentActivity() {

    private val viewModel: ExpenseViewModel by viewModels()
    private var initialDestination by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BudgetNotificationService.initNotificationChannel(this)
        BackupReminderService.initNotificationChannel(this)

        handleIncomingIntent(intent)

        if (viewModel.userPreferences.value.isBackupReminderEnabled) {
            BackupReminderService.schedulePeriodicReminder(this)
            viewModel.checkBackupReminderNow()
        }

        enableEdgeToEdge()
        setContent {
            val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()
            val isDarkTheme = when (userPreferences.themeMode) {
                "dark" -> true
                "light" -> false
                else -> if (userPreferences.isFollowSystemTheme) isSystemInDarkTheme() else userPreferences.isDarkMode
            }
            val appStrings = getAppStrings(userPreferences.language)

            CompositionLocalProvider(LocalAppStrings provides appStrings) {
                MyApplicationTheme(darkTheme = isDarkTheme) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNavigation(
                            viewModel = viewModel,
                            initialDestination = initialDestination
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val destination = intent?.getStringExtra(BackupReminderService.EXTRA_NAVIGATE_TO)
        if (!destination.isNullOrEmpty()) {
            initialDestination = destination
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.relockApp()
    }
}

