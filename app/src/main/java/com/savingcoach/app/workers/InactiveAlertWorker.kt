package com.savingcoach.app.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.savingcoach.app.core.notification.NotificationHelper
import com.savingcoach.app.data.repository.ExpenseRepository
import com.savingcoach.app.data.repository.SavingChallengeRepository
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
    private val savingChallengeRepository: SavingChallengeRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Inactive alerts are now handled by AlarmManager via ReminderReceiver
        return Result.success()
    }
}
