package com.savingcoach.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

import com.savingcoach.app.core.notification.NotificationScheduler
import javax.inject.Inject

@HiltAndroidApp
class SavingCoachApp : Application() {
    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    override fun onCreate() {
        super.onCreate()
        notificationScheduler.scheduleDailyReminders()
        notificationScheduler.scheduleBudgetCheck()
    }
}
