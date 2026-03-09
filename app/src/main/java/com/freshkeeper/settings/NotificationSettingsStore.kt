package com.freshkeeper.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationSettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun load(): NotificationSettings {
        return NotificationSettings(
            enabled = prefs.getBoolean(KEY_ENABLED, true),
            remindThreeDays = prefs.getBoolean(KEY_REMIND_THREE_DAYS, true),
            remindOneDay = prefs.getBoolean(KEY_REMIND_ONE_DAY, true),
            remindSameDay = prefs.getBoolean(KEY_REMIND_SAME_DAY, true),
            reminderHour = prefs.getInt(KEY_REMINDER_HOUR, 9),
        )
    }

    fun save(settings: NotificationSettings) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, settings.enabled)
            .putBoolean(KEY_REMIND_THREE_DAYS, settings.remindThreeDays)
            .putBoolean(KEY_REMIND_ONE_DAY, settings.remindOneDay)
            .putBoolean(KEY_REMIND_SAME_DAY, settings.remindSameDay)
            .putInt(KEY_REMINDER_HOUR, settings.reminderHour)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "freshkeeper_notification_settings"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_REMIND_THREE_DAYS = "remind_three_days"
        private const val KEY_REMIND_ONE_DAY = "remind_one_day"
        private const val KEY_REMIND_SAME_DAY = "remind_same_day"
        private const val KEY_REMINDER_HOUR = "reminder_hour"
    }
}

data class NotificationSettings(
    val enabled: Boolean = true,
    val remindThreeDays: Boolean = true,
    val remindOneDay: Boolean = true,
    val remindSameDay: Boolean = true,
    val reminderHour: Int = 9,
)
