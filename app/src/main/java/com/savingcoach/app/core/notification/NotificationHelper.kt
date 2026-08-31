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
import com.savingcoach.app.data.model.NotificationItem
import com.savingcoach.app.data.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Provider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

import com.savingcoach.app.data.repository.LanguagePreferences
import com.savingcoach.app.data.repository.AppLanguage
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class InAppNotification(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val type: String,
    val iconEmoji: String = when (type) {
        "BUDGET_BREACH" -> "⚠️"
        "SAVING_MILESTONE" -> "🏆"
        "ABANDONED_CHALLENGE" -> "🎯"
        "PORTFOLIO_RISK" -> "📉"
        "SEVERE_INACTIVITY" -> "👋"
        else -> "💡"
    }
)

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationRepositoryProvider: Provider<NotificationRepository>,
    private val languagePreferencesProvider: Provider<LanguagePreferences>
) {
    companion object {
        const val BUDGET_CHANNEL_ID = "budget_alerts"
        const val SAVING_CHANNEL_ID = "saving_milestones"
        const val DAILY_REMINDER_CHANNEL_ID = "daily_reminders"
    }

    // Use AtomicInteger to generate unique notification IDs without overflow
    private val notificationIdCounter = AtomicInteger(0)

    private val _inAppNotificationFlow = MutableSharedFlow<InAppNotification>(
        replay = 0,
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val inAppNotificationFlow: SharedFlow<InAppNotification> = _inAppNotificationFlow.asSharedFlow()

    private val isBurmese: Boolean
        get() = try {
            languagePreferencesProvider.get().language.value == AppLanguage.MY
        } catch (e: Exception) {
            false
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
        val title = if (isBurmese) "ဘတ်ဂျက် သတိပေးချက်" else "Budget Alert"
        val message = if (isBurmese) {
            "⚠️ သတိပေးချက်- သင်သည် လစဉ်ဘတ်ဂျက်၏ $percentage% သုံးစွဲပြီးပါပြီ။"
        } else {
            "⚠️ Budget Alert: You've reached $percentage% of your monthly limit."
        }
        
        showNotification(
            channelId = BUDGET_CHANNEL_ID,
            title = title,
            message = message,
            icon = R.drawable.ic_launcher_foreground,
            type = "BUDGET_BREACH"
        )
    }

    fun showSavingMilestone(challengeName: String, percentage: Int) {
        val title = when {
            percentage >= 100 -> if (isBurmese) "စိန်ခေါ်မှု အောင်မြင်ပါပြီ!" else "Challenge Complete!"
            percentage >= 75 -> if (isBurmese) "ပန်းတိုင်နီးပါပြီ!" else "Almost There!"
            percentage >= 50 -> if (isBurmese) "တစ်ဝက်ရောက်ပါပြီ!" else "Halfway!"
            else -> if (isBurmese) "စုဆောင်းမှု တိုးတက်မှု" else "Saving Progress"
        }

        val message = if (isBurmese) {
            "သင့် $challengeName ရည်မှန်းချက်၏ $percentage% ကို အောင်မြင်စွာ စုဆောင်းနိုင်ခဲ့ပါပြီ။"
        } else {
            "You've saved $percentage% of your $challengeName goal"
        }

        showNotification(
            channelId = SAVING_CHANNEL_ID,
            title = title,
            message = message,
            icon = R.drawable.ic_launcher_foreground,
            type = "SAVING_MILESTONE"
        )
    }

    fun showDailyReminder(message: String) {
        val title = "Saving Coach"
        showNotification(
            channelId = DAILY_REMINDER_CHANNEL_ID,
            title = title,
            message = message,
            icon = R.drawable.ic_launcher_foreground,
            type = "DAILY_REMINDER"
        )
    }

    fun showSevereInactivity() {
        val title = if (isBurmese) "သင့်ကို သတိရနေပါသည်!" else "We miss you!"
        val message = if (isBurmese) {
            "👋 မကြာသေးမီက အသုံးစရိတ်များကို စာရင်းသွင်းရန် စက္ကန့် ၃၀ ခန့် အချိန်ပေးပါ။"
        } else {
            "👋 We miss you! Take 30 seconds to catch up on your recent expenses."
        }
        showNotification(
            channelId = DAILY_REMINDER_CHANNEL_ID,
            title = title,
            message = message,
            icon = R.drawable.ic_launcher_foreground,
            type = "SEVERE_INACTIVITY"
        )
    }

    fun showAbandonedChallenge(challengeName: String) {
        val title = if (isBurmese) "စိန်ခေါ်မှု သတိပေးချက်" else "Challenge Alert"
        val message = if (isBurmese) {
            "🎯 $challengeName ကို ဆက်လက်လုပ်ဆောင်ပါ! ပန်းတိုင်ပြည့်ရန် အနည်းငယ်သာ လိုပါတော့သည်။"
        } else {
            "🎯 Keep your $challengeName alive! You're only a few check-ins away from this month's goal."
        }
        showNotification(
            channelId = SAVING_CHANNEL_ID,
            title = title,
            message = message,
            icon = R.drawable.ic_launcher_foreground,
            type = "ABANDONED_CHALLENGE"
        )
    }

    fun showPortfolioRiskAlert(assetName: String) {
        val title = if (isBurmese) "ဈေးကွက် သတင်း" else "Market Update"
        val message = if (isBurmese) {
            "📉 $assetName စျေးနှုန်း ယနေ့ သိသိသာသာ ကျဆင်းခဲ့သည်။ သင့် ရင်းနှီးမြှုပ်နှံမှု အစီအစဉ်ကို ပြန်လည်သုံးသပ်ပါ။"
        } else {
            "📉 Market Update: $assetName has dropped significantly today. Review your long-term DCA plan."
        }
        showNotification(
            channelId = DAILY_REMINDER_CHANNEL_ID,
            title = title,
            message = message,
            icon = R.drawable.ic_launcher_foreground,
            type = "PORTFOLIO_RISK"
        )
    }

    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale.getDefault()).format(amount)
    }

    private fun showNotification(channelId: String, title: String, message: String, icon: Int, type: String) {
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

        // 1. Emit in-app banner for on-screen display inside the app
        _inAppNotificationFlow.tryEmit(
            InAppNotification(
                title = title,
                message = message,
                type = type
            )
        )

        // 2. Only trigger OS system notification if the app is in the background
        // When in the foreground, the on-screen banner handles it so the dark OS heads-up doesn't overlap
        if (!AppLifecycleTracker.isAppInForeground) {
            createNotificationChannels()
            try {
                val notificationManagerCompat = androidx.core.app.NotificationManagerCompat.from(context)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    if (notificationManagerCompat.areNotificationsEnabled()) {
                        val notificationId = notificationIdCounter.incrementAndGet()
                        notificationManagerCompat.notify(notificationId, notification)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Save notification to Firestore history
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val notificationItem = NotificationItem(
                userId = userId,
                title = title,
                message = message,
                type = type,
                timestamp = System.currentTimeMillis(),
                isRead = false
            )
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    notificationRepositoryProvider.get().addNotification(userId, notificationItem)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
