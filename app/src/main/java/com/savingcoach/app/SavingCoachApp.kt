package com.savingcoach.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import com.savingcoach.app.core.notification.NotificationScheduler
import javax.inject.Inject

@HiltAndroidApp
class SavingCoachApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        com.savingcoach.app.core.notification.AppLifecycleTracker.register(this)
        notificationScheduler.scheduleDailyReminders()
        notificationScheduler.scheduleBudgetCheck()
        notificationScheduler.schedulePortfolioRiskCheck()
    }
}
