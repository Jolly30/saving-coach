package com.savingcoach.app.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.savingcoach.app.core.notification.NotificationHelper
import com.savingcoach.app.data.repository.SavingChallengeRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class SavingReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val savingChallengeRepository: SavingChallengeRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure()
            val challenges = savingChallengeRepository.getActiveChallenges(userId).first()

            if (challenges.isNotEmpty()) {
                val challengeNames = challenges.joinToString(", ") { it.title }
                notificationHelper.showDailyReminder(
                    "You have ${challenges.size} active saving challenges: $challengeNames. Keep going!"
                )
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
