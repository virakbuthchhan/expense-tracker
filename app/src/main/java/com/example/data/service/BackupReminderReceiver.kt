package com.example.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BackupReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        when (action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                BackupReminderService.schedulePeriodicReminder(context)
            }
            BackupReminderService.ACTION_TRIGGER_BACKUP_REMINDER -> {
                CoroutineScope(Dispatchers.IO).launch {
                    BackupReminderService.checkAndNotify(context)
                }
            }
        }
    }
}
