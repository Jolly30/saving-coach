package com.savingcoach.app.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.savingcoach.app.workers.BudgetAlertWorker
import com.savingcoach.app.workers.PortfolioRiskWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    companion object {
        const val BUDGET_CHECK_WORK = "budget_check_work"
        const val PORTFOLIO_RISK_WORK = "portfolio_risk_work"

        // Legacy WorkManager tasks that should be cancelled
        private const val LEGACY_DAILY_REMINDER_WORK = "daily_reminder_work"
        private const val LEGACY_SAVING_REMINDER_WORK = "saving_reminder_work"
        private const val LEGACY_INACTIVE_CHECK_WORK = "inactive_check_work"

        const val RC_SAVING_REMINDER = 201
        const val RC_EXPENSE_REMINDER = 202
        const val RC_INACTIVE_CHECK = 203
    }

    fun scheduleDailyReminders() {
        // 1. Cancel any legacy WorkManager daily reminder tasks so they never fire on app open
        try {
            workManager.cancelUniqueWork(LEGACY_DAILY_REMINDER_WORK)
            workManager.cancelUniqueWork(LEGACY_SAVING_REMINDER_WORK)
            workManager.cancelUniqueWork(LEGACY_INACTIVE_CHECK_WORK)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Schedule reliable alarms via AlarmManager for system tray & lockscreen display
        scheduleSavingReminder()
        scheduleExpenseReminder()
        scheduleInactiveCheck()
    }

    fun scheduleSavingReminder() {
        scheduleAlarm(
            action = ReminderReceiver.ACTION_SAVING_REMINDER,
            requestCode = RC_SAVING_REMINDER,
            hour = 19, // 7:00 PM
            minute = 0
        )
    }

    fun scheduleExpenseReminder() {
        scheduleAlarm(
            action = ReminderReceiver.ACTION_EXPENSE_REMINDER,
            requestCode = RC_EXPENSE_REMINDER,
            hour = 20, // 8:00 PM
            minute = 0
        )
    }

    fun scheduleInactiveCheck() {
        scheduleAlarm(
            action = ReminderReceiver.ACTION_INACTIVE_CHECK,
            requestCode = RC_INACTIVE_CHECK,
            hour = 21, // 9:00 PM
            minute = 0
        )
    }

    private fun scheduleAlarm(action: String, requestCode: Int, hour: Int, minute: Int) {
        val manager = alarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = calculateTriggerTime(hour, minute)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (manager.canScheduleExactAlarms()) {
                        manager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                    } else {
                        manager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                    }
                } else {
                    manager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                manager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            try {
                manager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun calculateTriggerTime(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }
        return target.timeInMillis
    }

    fun scheduleBudgetCheck() {
        val budgetCheckRequest = PeriodicWorkRequestBuilder<BudgetAlertWorker>(
            6, TimeUnit.HOURS // Check every 6 hours
        )
            .build()

        workManager.enqueueUniquePeriodicWork(
            BUDGET_CHECK_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            budgetCheckRequest
        )
    }

    fun schedulePortfolioRiskCheck() {
        val portfolioRiskRequest = PeriodicWorkRequestBuilder<PortfolioRiskWorker>(
            12, TimeUnit.HOURS // Check every 12 hours
        )
            .build()

        workManager.enqueueUniquePeriodicWork(
            PORTFOLIO_RISK_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            portfolioRiskRequest
        )
    }
}
