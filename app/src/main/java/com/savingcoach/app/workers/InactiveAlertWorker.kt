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
        return try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            
            // Get today, yesterday, and two days ago dates
            val calendar = Calendar.getInstance()
            val today = dateFormat.format(calendar.time)
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            val yesterday = dateFormat.format(calendar.time)
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            val twoDaysAgo = dateFormat.format(calendar.time)
            
            val dates = listOf(today, yesterday, twoDaysAgo)
            
            var hasActivity = false
            for (date in dates) {
                val expenses = expenseRepository.getExpensesForDate(userId, date).first()
                if (expenses.isNotEmpty()) {
                    hasActivity = true
                    break
                }
            }
            
            if (!hasActivity) {
                // Check active challenge deposits
                val activeChallenges = savingChallengeRepository.getActiveChallenges(userId).first()
                for (challenge in activeChallenges) {
                    val deposits = savingChallengeRepository.getDeposits(userId, challenge.id).first()
                    val recentDeposits = deposits.filter { it.date in dates }
                    if (recentDeposits.isNotEmpty()) {
                        hasActivity = true
                        break
                    }
                }
            }
            
            if (!hasActivity) {
                notificationHelper.showSevereInactivity()
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
