package com.example.data.service

import android.Manifest
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
import com.example.data.local.CategoryEntity
import com.example.data.local.PreferenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object BudgetNotificationService {

    const val CHANNEL_ID = "expense_budget_alerts"
    const val CHANNEL_NAME = "Budget & Spending Alerts"
    const val CHANNEL_DESCRIPTION = "Alerts when monthly category or total spending exceeds custom budget thresholds"

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
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

    suspend fun checkBudgetsAndNotify(
        context: Context,
        forceTest: Boolean = false
    ): Int = withContext(Dispatchers.IO) {
        initNotificationChannel(context)

        val preferenceRepo = PreferenceRepository(context)
        val userPreferences = preferenceRepo.userPreferences.value

        if (!userPreferences.isBudgetAlertsEnabled && !forceTest) {
            return@withContext 0
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                return@withContext 0
            }
        }

        val database = AppDatabase.getDatabase(context)
        val expenseDao = database.expenseDao()

        val calendar = Calendar.getInstance()
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val endOfMonth = calendar.timeInMillis - 1

        val monthlyBudgets = expenseDao.getBudgetsForMonth(currentMonth).firstOrNull() ?: emptyList()
        if (monthlyBudgets.isEmpty() && !forceTest) {
            return@withContext 0
        }

        val categorySpendingList = expenseDao.getCategorySpendingInRange(startOfMonth, endOfMonth).firstOrNull() ?: emptyList()
        val spendingMap = categorySpendingList.associate { it.categoryId to it.totalAmount }
        val allCategories = expenseDao.getAllCategories().firstOrNull() ?: emptyList()
        val categoryMap: Map<Int, CategoryEntity> = allCategories.associateBy { it.id }

        val thresholdPercent = userPreferences.budgetAlertThresholdPercent
        val currencySymbol = userPreferences.currencySymbol
        var notificationsSent = 0

        for (budget in monthlyBudgets) {
            val spent = spendingMap[budget.categoryId] ?: 0.0
            val limit = budget.monthlyLimit
            if (limit <= 0.0) continue

            val ratio = (spent / limit) * 100.0

            if (ratio >= thresholdPercent) {
                val category = categoryMap[budget.categoryId]
                val categoryName = category?.name ?: "Category #${budget.categoryId}"
                val ratioRounded = ratio.roundToInt()
                val spentFormatted = String.format(Locale.US, "%.2f", spent)
                val limitFormatted = String.format(Locale.US, "%.2f", limit)

                val (title, body) = if (ratio >= 100.0) {
                    Pair(
                        "🚨 Budget Exceeded: $categoryName",
                        "You've spent $currencySymbol$spentFormatted of your $currencySymbol$limitFormatted budget ($ratioRounded%)."
                    )
                } else {
                    Pair(
                        "⚠️ Budget Alert: $categoryName ($ratioRounded%)",
                        "You've used $ratioRounded% of your $currencySymbol$limitFormatted monthly budget ($currencySymbol$spentFormatted spent)."
                    )
                }

                val notificationId = 1000 + budget.categoryId
                sendNotification(context, notificationId, title, body)
                notificationsSent++
            }
        }

        notificationsSent
    }

    fun sendTestNotification(
        context: Context,
        customThreshold: Int? = null
    ) {
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
        val threshold = customThreshold ?: prefs.budgetAlertThresholdPercent
        val currency = prefs.currencySymbol

        val title = "⚠️ Budget Alert: Food & Dining ($threshold%)"
        val body = "Sample warning: You've reached $threshold% of your $currency" + "350.00 monthly budget."

        sendNotification(context, 9999, title, body)
    }

    private fun sendNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission not granted or notification disabled
        }
    }
}
