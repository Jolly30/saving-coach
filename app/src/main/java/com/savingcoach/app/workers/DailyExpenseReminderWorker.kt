package com.savingcoach.app.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.savingcoach.app.core.notification.NotificationHelper
import com.savingcoach.app.data.repository.ExpenseRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltWorker
class DailyExpenseReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val expenseRepository: ExpenseRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val todayExpenses = expenseRepository.getExpensesForDate(userId, today).first()

            if (todayExpenses.isEmpty()) {
                notificationHelper.showDailyReminder(
                    "Don't forget to log your expenses today!"
                )
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
