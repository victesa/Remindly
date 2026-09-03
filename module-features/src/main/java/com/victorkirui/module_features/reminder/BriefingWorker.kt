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
    private val authRepository: com.victorkirui.core.repository.AuthRepository by inject()
    private val notificationHelper: NotificationHelper by inject()
    private val settingsRepository: ReminderSettingsRepository by inject()

    override suspend fun doWork(): Result {
        val userId = authRepository.currentUser.value?.uid ?: return Result.failure()
        
        // Periodic Cleanup
        localRepository.deleteExpiredItems(userId)

        val isEnabled = settingsRepository.isMorningBriefingEnabled.first()
        if (!isEnabled) {
            android.util.Log.d("BriefingWorker", "Morning briefing is disabled in settings.")
            return Result.success()
        }

        val today = LocalDate.now()
        val remindersToday = localRepository.getAllRemindersWithItems(userId).first().filter {
            try {
                val reminderDate = LocalDateTime.parse(it.reminder.reminderDateTime).toLocalDate()
                reminderDate.isEqual(today) && it.item.status != "DONE"
            } catch (e: Exception) {
                it.reminder.reminderDateTime.startsWith(today.toString()) && it.item.status != "DONE"
            }
        }

        if (remindersToday.isNotEmpty()) {
            val count = remindersToday.size
            val (title, message) = if (count == 1) {
                "Today's Goal" to "You have one item needing your attention today: ${remindersToday.first().item.title}. You've got this!"
            } else {
                "Daily Briefing" to "You have $count things lined up for today. Try to work on them before their deadlines."
            }
            // Use a fixed notification ID for briefings so they replace each other
            notificationHelper.showNotification(title, message, notificationId = 1001)
            android.util.Log.d("BriefingWorker", "Sent morning briefing with $count items.")
        } else {
            val engagementMessages = listOf(
                "Your schedule is clear! It’s a great time to explore new opportunities.",
                "No deadlines today. Why not take a few minutes to organize your upcoming goals?",
                "A clear day ahead! Keep sharing interesting captures with Remindly.",
                "Nothing on the list for today. Spend some time searching for your next big thing."
            )
            val randomMessage = engagementMessages.random()
            notificationHelper.showNotification("Plan Your Day", randomMessage, notificationId = 1001)
            android.util.Log.d("BriefingWorker", "Sent engagement notification.")
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "MorningBriefingWork"

        fun schedule(context: Context, preferredTime: LocalTime) {
            val now = LocalTime.now()
            var delayMinutes = java.time.Duration.between(now, preferredTime).toMinutes()
            
            // If the time has already passed today, schedule for tomorrow
            if (delayMinutes <= 0) {
                delayMinutes += 24 * 60
            }

            android.util.Log.d("BriefingWorker", "Scheduling morning briefing in $delayMinutes minutes (at $preferredTime)")

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<BriefingWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
