package com.victorkirui.module_features.reminder

import android.content.Context
import androidx.work.*
import com.victorkirui.core.notification.NotificationHelper
import com.victorkirui.core.repository.ReminderSettingsRepository
import com.victorkirui.local.repository.LocalRepository
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class BriefingWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val localRepository: LocalRepository by inject()
    private val notificationHelper: NotificationHelper by inject()
    private val settingsRepository: ReminderSettingsRepository by inject()

    override suspend fun doWork(): Result {
        val isEnabled = settingsRepository.isMorningBriefingEnabled.first()
        if (!isEnabled) return Result.success()

        val today = LocalDate.now().toString()
        val remindersToday = localRepository.getAllRemindersWithItems().first().filter {
            it.reminder.reminderDateTime.startsWith(today)
        }

        if (remindersToday.isNotEmpty()) {
            val count = remindersToday.size
            val message = if (count == 1) {
                "You have 1 thing needing attention today."
            } else {
                "You have $count things needing attention today."
            }
            notificationHelper.showNotification("Morning Briefing", message)
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "MorningBriefingWork"

        fun schedule(context: Context, preferredTime: LocalTime) {
            val now = LocalTime.now()
            var delay = java.time.Duration.between(now, preferredTime).toMinutes()
            if (delay < 0) delay += 24 * 60

            val constraints = Constraints.Builder()
                .build()

            val workRequest = PeriodicWorkRequestBuilder<BriefingWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }
    }
}
