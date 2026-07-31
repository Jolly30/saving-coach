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
import java.util.Calendar
import java.util.Locale

@HiltWorker
class InactiveAlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val expenseRepository: ExpenseRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure()
            val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }.time
            )

            val yesterdayExpenses = expenseRepository.getExpensesForDate(userId, yesterday).first()

            if (yesterdayExpenses.isEmpty()) {
                notificationHelper.showDailyReminder(
                    "You haven't logged expenses yesterday. Stay on track with your budget!"
                )
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
