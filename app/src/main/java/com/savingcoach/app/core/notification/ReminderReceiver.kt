package com.savingcoach.app.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.savingcoach.app.data.repository.ExpenseRepository
import com.savingcoach.app.data.repository.SavingChallengeRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SAVING_REMINDER = "com.savingcoach.app.ACTION_SAVING_REMINDER"
        const val ACTION_EXPENSE_REMINDER = "com.savingcoach.app.ACTION_EXPENSE_REMINDER"
        const val ACTION_INACTIVE_CHECK = "com.savingcoach.app.ACTION_INACTIVE_CHECK"
    }

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    @Inject
    lateinit var expenseRepository: ExpenseRepository

    @Inject
    lateinit var savingChallengeRepository: SavingChallengeRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_EXPENSE_REMINDER -> handleExpenseReminder()
                    ACTION_SAVING_REMINDER -> handleSavingReminder()
                    ACTION_INACTIVE_CHECK -> handleInactiveCheck()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleExpenseReminder() {
        // Reschedule for tomorrow
        notificationScheduler.scheduleExpenseReminder()

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        try {
            val todayExpenses = expenseRepository.getExpensesForDate(userId, today).first()
            if (todayExpenses.isEmpty()) {
                val message = if (notificationHelper.isBurmeseLanguage) {
                    "ယနေ့ အသုံးစရိတ်များကို စာရင်းသွင်းရန် မမေ့ပါနှင့်!"
                } else {
                    "Don't forget to log your expenses today!"
                }
                notificationHelper.showDailyExpenseReminder(message)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun handleSavingReminder() {
        // Reschedule for tomorrow
        notificationScheduler.scheduleSavingReminder()

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        try {
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
                    break
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
                notificationHelper.showDailySavingReminder(message)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun handleInactiveCheck() {
        // Reschedule for tomorrow
        notificationScheduler.scheduleInactiveCheck()

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val calendar = Calendar.getInstance()
        val today = dateFormat.format(calendar.time)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val yesterday = dateFormat.format(calendar.time)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val twoDaysAgo = dateFormat.format(calendar.time)

        val dates = listOf(today, yesterday, twoDaysAgo)

        try {
            var hasActivity = false
            for (date in dates) {
                val expenses = expenseRepository.getExpensesForDate(userId, date).first()
                if (expenses.isNotEmpty()) {
                    hasActivity = true
                    break
                }
            }

            if (!hasActivity) {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
