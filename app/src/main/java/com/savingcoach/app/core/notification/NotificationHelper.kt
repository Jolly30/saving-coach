package com.savingcoach.app.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.savingcoach.app.MainActivity
import com.savingcoach.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val BUDGET_CHANNEL_ID = "budget_alerts"
        const val SAVING_CHANNEL_ID = "saving_milestones"
        const val DAILY_REMINDER_CHANNEL_ID = "daily_reminders"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val budgetChannel = NotificationChannel(
                BUDGET_CHANNEL_ID,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for budget threshold alerts"
                enableVibration(true)
            }

            val savingChannel = NotificationChannel(
                SAVING_CHANNEL_ID,
                "Saving Milestones",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for saving goal achievements"
            }

            val reminderChannel = NotificationChannel(
                DAILY_REMINDER_CHANNEL_ID,
                "Daily Reminders",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Daily reminders to log expenses and check savings"
                enableVibration(false)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannels(
                listOf(budgetChannel, savingChannel, reminderChannel)
            )
        }
    }

    fun showBudgetAlert(percentage: Int, overspent: Double = 0.0) {
        val title = if (overspent > 0) "Over Budget!" else "Budget Alert"
        val message = if (overspent > 0) {
            "You've exceeded your budget by ${formatCurrency(overspent)}"
        } else {
            "You've used $percentage% of your monthly budget"
        }

        showNotification(
            channelId = BUDGET_CHANNEL_ID,
            title = title,
            message = message,
            icon = R.drawable.ic_launcher_foreground // fallback since ic_budget_alert doesn't exist yet
        )
    }

    fun showSavingMilestone(challengeName: String, percentage: Int) {
        val title = when {
            percentage >= 100 -> "Challenge Complete!"
            percentage >= 75 -> "Almost There!"
            percentage >= 50 -> "Halfway!"
            else -> "Saving Progress"
        }

        showNotification(
            channelId = SAVING_CHANNEL_ID,
            title = title,
            message = "You've saved $percentage% of your $challengeName goal",
            icon = R.drawable.ic_launcher_foreground
        )
    }

    fun showDailyReminder(message: String) {
        showNotification(
            channelId = DAILY_REMINDER_CHANNEL_ID,
            title = "Saving Coach",
            message = message,
            icon = R.drawable.ic_launcher_foreground
        )
    }

    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale.getDefault()).format(amount)
    }

    private fun showNotification(channelId: String, title: String, message: String, icon: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
