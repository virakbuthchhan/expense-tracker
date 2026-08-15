package com.example.data.service

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.local.PreferenceRepository
import com.example.ui.i18n.getAppStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

object BackupReminderService {

    const val CHANNEL_ID = "expense_backup_reminders"
    const val CHANNEL_NAME = "Backup & Export Reminders"
    const val CHANNEL_DESCRIPTION = "Periodic reminders to export and backup your financial transaction records"

    const val EXTRA_NAVIGATE_TO = "navigate_to_destination"
    const val DESTINATION_EXPORT = "export"

    const val ACTION_TRIGGER_BACKUP_REMINDER = "com.example.action.TRIGGER_BACKUP_REMINDER"
    const val NOTIFICATION_ID_BACKUP_REMINDER = 7777
    const val NOTIFICATION_ID_TEST_REMINDER = 7778

    private const val ALARM_REQUEST_CODE = 9001

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun schedulePeriodicReminder(context: Context) {
        val preferenceRepo = PreferenceRepository(context)
        val prefs = preferenceRepo.userPreferences.value

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, BackupReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_BACKUP_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!prefs.isBackupReminderEnabled) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val intervalMillis = when (prefs.backupReminderFrequency) {
            "daily" -> AlarmManager.INTERVAL_DAY
            "weekly" -> AlarmManager.INTERVAL_DAY * 7L
            "biweekly" -> AlarmManager.INTERVAL_DAY * 14L
            "monthly" -> AlarmManager.INTERVAL_DAY * 30L
            else -> AlarmManager.INTERVAL_DAY * 7L
        }

        val triggerAtMillis = System.currentTimeMillis() + intervalMillis

        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                intervalMillis,
                pendingIntent
            )
        } catch (_: Exception) {
            // In case of alarm manager permission restrictions
        }
    }

    fun cancelPeriodicReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, BackupReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_BACKUP_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    suspend fun checkAndNotify(
        context: Context,
        forceTest: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        initNotificationChannel(context)

        val preferenceRepo = PreferenceRepository(context)
        val prefs = preferenceRepo.userPreferences.value

        if (!prefs.isBackupReminderEnabled && !forceTest) {
            return@withContext false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                return@withContext false
            }
        }

        val intervalMillis = when (prefs.backupReminderFrequency) {
            "daily" -> 24 * 60 * 60 * 1000L
            "weekly" -> 7 * 24 * 60 * 60 * 1000L
            "biweekly" -> 14 * 24 * 60 * 60 * 1000L
            "monthly" -> 30 * 24 * 60 * 60 * 1000L
            else -> 7 * 24 * 60 * 60 * 1000L
        }

        val now = System.currentTimeMillis()
        val elapsed = now - prefs.lastExportTimestamp

        // Check if database has any transactions recorded
        val db = AppDatabase.getDatabase(context)
        val txCount = db.expenseDao().getAllTransactionsWithCategory().firstOrNull()?.size ?: 0

        if (!forceTest) {
            if (txCount == 0) return@withContext false
            if (elapsed < intervalMillis) return@withContext false
        }

        val strings = getAppStrings(prefs.language)
        val title = strings.backupNotificationTitle
        val body = strings.backupNotificationBody

        sendNotification(
            context = context,
            notificationId = if (forceTest) NOTIFICATION_ID_TEST_REMINDER else NOTIFICATION_ID_BACKUP_REMINDER,
            title = title,
            body = body
        )

        true
    }

    fun sendTestNotification(context: Context) {
        initNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val preferenceRepo = PreferenceRepository(context)
        val prefs = preferenceRepo.userPreferences.value
        val strings = getAppStrings(prefs.language)

        sendNotification(
            context = context,
            notificationId = NOTIFICATION_ID_TEST_REMINDER,
            title = strings.backupNotificationTitle,
            body = strings.backupNotificationBody
        )
    }

    private fun sendNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_NAVIGATE_TO, DESTINATION_EXPORT)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Notification permission might not be granted
        }
    }
}
