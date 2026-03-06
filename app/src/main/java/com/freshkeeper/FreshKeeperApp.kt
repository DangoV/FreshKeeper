package com.freshkeeper

import android.app.Application
import android.app.NotificationManager
import com.freshkeeper.reminders.ReminderNotification
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FreshKeeperApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val notificationManager = getSystemService(NotificationManager::class.java)
        ReminderNotification.ensureChannel(notificationManager)
    }
}
