package com.freshkeeper.reminders

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun scheduleProductReminders(
        productId: Long,
        productName: String,
        expiryDate: LocalDate,
    ) {
        listOf(3L, 1L, 0L).forEach { daysBefore ->
            val triggerDateTime = expiryDate.minusDays(daysBefore).atTime(9, 0)
            val delay = Duration.between(LocalDateTime.now(), triggerDateTime)

            if (delay.isNegative || delay.isZero) {
                return@forEach
            }

            val request = OneTimeWorkRequestBuilder<ExpiryReminderWorker>()
                .setInitialDelay(delay)
                .setInputData(
                    workDataOf(
                        ExpiryReminderWorker.KEY_PRODUCT_ID to productId,
                        ExpiryReminderWorker.KEY_PRODUCT_NAME to productName,
                        ExpiryReminderWorker.KEY_DAYS_BEFORE to daysBefore,
                    ),
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueWorkName(productId, daysBefore),
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }

    private fun uniqueWorkName(productId: Long, daysBefore: Long): String {
        return "expiry-reminder-$productId-$daysBefore"
    }
}
