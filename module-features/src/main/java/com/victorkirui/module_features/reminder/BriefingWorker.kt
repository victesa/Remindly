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
        val allItems = localRepository.getAllItems(userId).first().filter {
            it.status != "DONE" && it.status != "FAILED"
        }

        val pendingCount = allItems.size
        val dueTodayItems = allItems.filter { item ->
            val deadlineDate = com.victorkirui.core.util.DateUtils.parseToLocalDate(item.deadline)
            val eventDate = com.victorkirui.core.util.DateUtils.parseToLocalDate(item.eventDate)
            (deadlineDate?.isEqual(today) == true) || (eventDate?.isEqual(today) == true)
        }
        val dueTodayCount = dueTodayItems.size

        val (title, message) = when {
            dueTodayCount > 0 -> {
                val dueText = if (dueTodayCount == 1) {
                    "1 item due today: \"${dueTodayItems.first().title}\""
                } else {
                    "$dueTodayCount items due today"
                }
                val pendingText = if (pendingCount == dueTodayCount) {
                    ""
                } else {
                    " ($pendingCount total pending)"
                }
                "Daily Briefing" to "$dueText$pendingText. Stay on top of your goals!"
            }
            pendingCount > 0 -> {
                val pendingStr = if (pendingCount == 1) "1 pending item" else "$pendingCount pending items"
                "Daily Briefing" to "No deadlines due today. You have $pendingStr in your pipeline."
            }
            else -> {
                val engagementMessages = listOf(
                    "Your schedule is clear today! Great time to capture new opportunities.",
                    "No pending items or deadlines today. Enjoy your day!",
                    "All caught up! Keep capturing interesting notes and events with Remindly.",
                    "A clear day ahead! Explore new goals and opportunities."
                )
                "Plan Your Day" to engagementMessages.random()
            }
        }

        notificationHelper.showNotification(title, message, notificationId = 1001)
        android.util.Log.d("BriefingWorker", "Sent morning briefing: dueToday=$dueTodayCount, pending=$pendingCount")

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
