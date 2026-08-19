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
import java.time.LocalDateTime
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
        if (!isEnabled) {
            android.util.Log.d("BriefingWorker", "Morning briefing is disabled in settings.")
            return Result.success()
        }

        val today = LocalDate.now()
        val remindersToday = localRepository.getAllRemindersWithItems().first().filter {
            try {
                val reminderDate = LocalDateTime.parse(it.reminder.reminderDateTime).toLocalDate()
                reminderDate.isEqual(today) && it.item.status != "DONE"
            } catch (e: Exception) {
                it.reminder.reminderDateTime.startsWith(today.toString()) && it.item.status != "DONE"
            }
        }

        if (remindersToday.isNotEmpty()) {
            val count = remindersToday.size
            val message = if (count == 1) {
                "You have 1 thing needing attention today: ${remindersToday.first().item.title}"
            } else {
                "You have $count things needing attention today."
            }
            notificationHelper.showNotification("Morning Briefing", message)
            android.util.Log.d("BriefingWorker", "Sent morning briefing with $count items.")
        } else {
            val engagementMessages = listOf(
                "Your schedule is clear! It’s a great time to explore new opportunities and capture them in Remindly.",
                "No deadlines today. Why not take a few minutes to organize your upcoming goals?",
                "A clear day ahead! Remember to share interesting job posts or events to Remindly so you don't forget them.",
                "Nothing on the list for today. Spend some time searching for your next big thing and let Remindly handle the reminders."
            )
            val randomMessage = engagementMessages.random()
            notificationHelper.showNotification("Plan Your Day", randomMessage)
            android.util.Log.d("BriefingWorker", "Sent engagement notification as there were no reminders.")
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
