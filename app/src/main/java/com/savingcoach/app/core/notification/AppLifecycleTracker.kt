package com.savingcoach.app.core.notification

import android.app.Activity
import android.app.Application
import android.os.Bundle

object AppLifecycleTracker {
    @Volatile
    var isAppInForeground: Boolean = false
        private set

    private var activeActivities = 0

    fun register(app: Application) {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {
                activeActivities++
                isAppInForeground = activeActivities > 0
            }
            override fun onActivityResumed(activity: Activity) {
                isAppInForeground = true
            }
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                activeActivities = maxOf(0, activeActivities - 1)
                isAppInForeground = activeActivities > 0
            }
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
