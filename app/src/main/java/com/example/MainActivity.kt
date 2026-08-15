package com.example

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
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

fun Context.findMainActivity(): MainActivity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is MainActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

class MainActivity : FragmentActivity() {

    companion object {
        const val REQUEST_CODE_PICK_IMPORT_FILE = 1001
        const val REQUEST_CODE_CREATE_PDF_DOC = 1002
    }

    private var onFilePickedListener: ((Uri?) -> Unit)? = null
    private var onDocumentCreatedListener: ((Uri?) -> Unit)? = null

    private val viewModel: ExpenseViewModel by viewModels()
    private var initialDestination by mutableStateOf<String?>(null)

    fun launchFilePicker(callback: (Uri?) -> Unit) {
        this.onFilePickedListener = callback
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            val mimeTypes = arrayOf(
                "text/tab-separated-values",
                "text/comma-separated-values",
                "text/csv",
                "text/plain",
                "application/vnd.ms-excel",
                "application/csv",
                "*/*"
            )
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        try {
            startActivityForResult(Intent.createChooser(intent, "Select TSV or CSV File"), REQUEST_CODE_PICK_IMPORT_FILE)
        } catch (e: Exception) {
            try {
                startActivityForResult(intent, REQUEST_CODE_PICK_IMPORT_FILE)
            } catch (e2: Exception) {
                callback(null)
            }
        }
    }

    fun launchCreateDocument(mimeType: String, defaultFileName: String, callback: (Uri?) -> Unit) {
        this.onDocumentCreatedListener = callback
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, defaultFileName)
        }
        try {
            startActivityForResult(intent, REQUEST_CODE_CREATE_PDF_DOC)
        } catch (e: Exception) {
            callback(null)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_PICK_IMPORT_FILE -> {
                val uri = if (resultCode == Activity.RESULT_OK) data?.data else null
                onFilePickedListener?.invoke(uri)
                onFilePickedListener = null
            }
            REQUEST_CODE_CREATE_PDF_DOC -> {
                val uri = if (resultCode == Activity.RESULT_OK) data?.data else null
                onDocumentCreatedListener?.invoke(uri)
                onDocumentCreatedListener = null
            }
        }
    }

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

