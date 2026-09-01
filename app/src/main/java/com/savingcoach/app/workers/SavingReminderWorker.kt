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
            var triggeredAbandoned = false
            
            val fiveDaysInMillis = 5L * 24 * 60 * 60 * 1000L
            val now = System.currentTimeMillis()
            
            for (challenge in challenges) {
                val deposits = savingChallengeRepository.getDeposits(userId, challenge.id).first()
                val lastActivityTime = if (deposits.isNotEmpty()) {
                    deposits.first().createdAt
                } else {
                    challenge.createdAt
                }
                
                if (now - lastActivityTime >= fiveDaysInMillis) {
                    notificationHelper.showAbandonedChallenge(challenge.title)
                    triggeredAbandoned = true
                    break // Only show one alert at a time to prevent notification spam
                }
            }
            
            if (!triggeredAbandoned && challenges.isNotEmpty()) {
                val count = challenges.size
                val message = if (notificationHelper.isBurmeseLanguage) {
                    "သင့်တွင် လုပ်ဆောင်နေသော ငွေစုစိန်ခေါ်မှု $count ခု ရှိပါသည်။ ဆက်လက်ကြိုးစားပါ!"
                } else {
                    if (count == 1) {
                        "You have 1 active saving challenge. Keep going!"
                    } else {
                        "You have $count active saving challenges. Keep going!"
                    }
                }
                notificationHelper.showDailyReminder(message)
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
