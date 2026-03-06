package com.freshkeeper.reminders

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ExpiryReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val productName = inputData.getString(KEY_PRODUCT_NAME) ?: return Result.failure()
        val daysBefore = inputData.getLong(KEY_DAYS_BEFORE, -1L)
        val customMessage = inputData.getString(KEY_CUSTOM_MESSAGE)

        val contentText = customMessage ?: when (daysBefore) {
            3L -> "У продукта $productName истекает срок через 3 дня"
            1L -> "У продукта $productName истекает срок завтра"
            0L -> "У продукта $productName срок годности сегодня"
            else -> "Проверьте срок годности: $productName"
        }

        val notification = NotificationCompat.Builder(applicationContext, ReminderNotification.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("FreshKeeper")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            notification,
        )

        return Result.success()
    }

    companion object {
        const val KEY_PRODUCT_ID = "product_id"
        const val KEY_PRODUCT_NAME = "product_name"
        const val KEY_DAYS_BEFORE = "days_before"
        const val KEY_CUSTOM_MESSAGE = "custom_message"
    }
}
