package com.freshkeeper.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

object ReminderNotification {
    const val CHANNEL_ID = "freshkeeper_expiry_reminders"

    fun ensureChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Напоминания FreshKeeper",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Уведомления о скором окончании срока годности"
        }

        notificationManager.createNotificationChannel(channel)
    }
}
