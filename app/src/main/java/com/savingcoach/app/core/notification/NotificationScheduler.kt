package com.savingcoach.app.core.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.savingcoach.app.workers.BudgetAlertWorker
import com.savingcoach.app.workers.DailyExpenseReminderWorker
import com.savingcoach.app.workers.InactiveAlertWorker
import com.savingcoach.app.workers.SavingReminderWorker
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

    companion object {
        private const val DAILY_REMINDER_WORK = "daily_reminder_work"
        private const val BUDGET_CHECK_WORK = "budget_check_work"
        private const val SAVING_REMINDER_WORK = "saving_reminder_work"
        private const val INACTIVE_CHECK_WORK = "inactive_check_work"
        private const val PORTFOLIO_RISK_WORK = "portfolio_risk_work"
    }

    fun scheduleDailyReminders() {
        // Daily expense reminder at 8:00 PM
        val expenseReminderRequest = PeriodicWorkRequestBuilder<DailyExpenseReminderWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(calculateDelay(20, 0), TimeUnit.MILLISECONDS) // 8:00 PM
            .build()

        // Daily saving challenge reminder at 7:00 PM
        val savingReminderRequest = PeriodicWorkRequestBuilder<SavingReminderWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(calculateDelay(19, 0), TimeUnit.MILLISECONDS) // 7:00 PM
            .build()

        // Inactive alert at 9:00 PM
        val inactiveAlertRequest = PeriodicWorkRequestBuilder<InactiveAlertWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(calculateDelay(21, 0), TimeUnit.MILLISECONDS) // 9:00 PM
            .build()

        workManager.enqueueUniquePeriodicWork(
            DAILY_REMINDER_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            expenseReminderRequest
        )

        workManager.enqueueUniquePeriodicWork(
            SAVING_REMINDER_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            savingReminderRequest
        )

        workManager.enqueueUniquePeriodicWork(
            INACTIVE_CHECK_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            inactiveAlertRequest
        )
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

    private fun calculateDelay(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}
