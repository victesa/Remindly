package com.victorkirui.module_features.capturing

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.*
import com.victorkirui.core.util.DateUtils
import com.victorkirui.local.repository.LocalRepository
import com.victorkirui.module_features.capturing.ai.AiExtractor
import com.victorkirui.module_features.reminder.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class SyncWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val localRepository: LocalRepository by inject()
    private val authRepository: com.victorkirui.core.repository.AuthRepository by inject()
    private val aiExtractor: AiExtractor by inject()
    private val reminderScheduler: ReminderScheduler by inject()
    private val pdfTextExtractor: PdfTextExtractor by inject()
    private val subscriptionRepository: com.victorkirui.core.repository.SubscriptionRepository by inject()
    private val calendarManager: com.victorkirui.core.calendar.CalendarManager by inject()
    private val captureApiService: com.victorkirui.remote.CaptureApiService by inject()

    override suspend fun doWork(): Result {
        // Cleanup expired items first
        val currentUserId = authRepository.currentUser.value?.uid
        if (currentUserId != null) {
            localRepository.deleteExpiredItems(currentUserId)
        }

        // Process Pending Deletions
        val pendingDeletions = localRepository.getAllPendingDeletions()
        for (deletion in pendingDeletions) {
            try {
                val success = captureApiService.deleteItem(deletion.itemId)
                if (success) {
                    localRepository.removePendingDeletion(deletion.itemId)
                }
            } catch (e: Exception) {
                Log.e("SyncWorker", "Failed to delete item ${deletion.itemId} from backend", e)
            }
        }

        val pendingSyncs = localRepository.getAllPendingSyncs()
        if (pendingSyncs.isEmpty()) return Result.success()

        var hasFailures = false

        for (sync in pendingSyncs) {
            try {
                val item = localRepository.getItem(sync.itemId) ?: run {
                    localRepository.removePendingSync(sync.itemId)
                    continue
                }

                val mediaUri = item.originalMediaUri?.let { Uri.parse(it) }
                
                var extractedText = item.extractedText
                if (extractedText == null && mediaUri != null) {
                    extractedText = if (item.contentType == "DOCUMENT") {
                        pdfTextExtractor.extractText(mediaUri)
                    } else {
                        pdfTextExtractor.extractTextFromImage(mediaUri)
                    }
                }

                val aiInput = com.victorkirui.module_features.capturing.ai.AiInput(
                    text = extractedText,
                    url = item.sourceUrl,
                    mediaUri = if (item.contentType == "DOCUMENT") null else mediaUri, // Rule: Do not upload raw PDF as image
                    contentType = item.contentType,
                    idempotencyKey = "${item.id}_${System.currentTimeMillis()}"
                )


                val result = aiExtractor.extract(aiInput)
                
                // CRITICAL: Check if item still exists before saving results.
                val currentItem = localRepository.getItem(item.id)
                if (currentItem == null) {
                    Log.d("SyncWorker", "Item ${item.id} was deleted while processing. Aborting save.")
                    localRepository.removePendingSync(item.id)
                    continue
                }

                if (result is com.victorkirui.module_features.capturing.ai.AiResult.Success) {
                    val cleanDeadline = result.deadline?.takeIf { 
                        it.isNotBlank() && !it.equals("null", ignoreCase = true) && !it.equals("none", ignoreCase = true) && !it.equals("n/a", ignoreCase = true)
                    }
                    val rawEventDate = result.eventDate?.takeIf { 
                        it.isNotBlank() && !it.equals("null", ignoreCase = true) && !it.equals("none", ignoreCase = true) && !it.equals("n/a", ignoreCase = true)
                    }
                    val cleanEventDate = DateUtils.sanitizeEventDate(rawEventDate, cleanDeadline)

                    val updatedMetadata = (item.metadata ?: emptyMap()).toMutableMap().apply {
                        put("strategy", result.strategy)
                        put("synced", "true")
                        put("analyzed_at", java.time.Instant.now().toString())
                        result.id?.let { put("cloud_id", it) }
                    }
                    val updatedItem = item.copy(
                        id = result.id ?: item.id,
                        title = result.title,
                        summary = result.summary,
                        category = result.category ?: "OTHER",
                        deadline = cleanDeadline,
                        eventDate = cleanEventDate,
                        organization = result.organization,
                        checklist = result.checklist,
                        status = "READY",
                        metadata = updatedMetadata
                    )

                    localRepository.withTransaction {
                        if (updatedItem.id != item.id) {
                            localRepository.deleteItem(item.userId, item.id)
                        }
                        localRepository.saveItem(updatedItem)
                        reminderScheduler.scheduleRemindersForItem(updatedItem)
                        localRepository.removePendingSync(item.id)
                    }

                    // AUTO-SYNC TO CALENDAR (Premium Only)
                    val tier = subscriptionRepository.subscriptionTier.first()
                    val calendarSyncEnabled = subscriptionRepository.isCalendarSyncEnabled.first()
                    if (tier == com.victorkirui.core.repository.SubscriptionTier.PREMIUM && calendarSyncEnabled) {
                        val calendarEventDate = updatedItem.eventDate ?: updatedItem.deadline
                        if (calendarEventDate != null) {
                            try {
                                val uri = calendarManager.addEvent(
                                    title = updatedItem.title,
                                    description = updatedItem.summary,
                                    eventDate = calendarEventDate,
                                    location = updatedItem.organization
                                )
                                if (uri != null) {
                                    val meta = updatedItem.metadata?.toMutableMap() ?: mutableMapOf()
                                    meta["google_calendar_synced"] = "true"
                                    meta["calendar_event_uri"] = uri.toString()
                                    localRepository.saveItem(updatedItem.copy(metadata = meta))
                                }
                            } catch (e: Exception) {
                                Log.e("SyncWorker", "Auto-sync failed", e)
                            }
                        }
                    }
                } else if (result is com.victorkirui.module_features.capturing.ai.AiResult.Error && !result.isTransient) {
                    // Permanent failure during sync
                    val failedItem = item.copy(
                        title = "Analysis Failed",
                        summary = result.message,
                        status = "FAILED"
                    )
                    localRepository.withTransaction {
                        localRepository.saveItem(failedItem)
                        localRepository.removePendingSync(item.id)
                    }
                } else {
                    val retryingItem = item.copy(status = "RETRYING")
                    localRepository.saveItem(retryingItem)
                    hasFailures = true
                }
            } catch (e: Exception) {
                Log.e("SyncWorker", "Failed to sync item ${sync.itemId}", e)
                val isTransient = e is java.io.IOException || (e is retrofit2.HttpException && e.code() in 500..599)
                if (isTransient) {
                    hasFailures = true
                } else {
                    // Permanent failure, mark as failed
                    localRepository.getItem(sync.itemId)?.let {
                        localRepository.saveItem(it.copy(status = "FAILED"))
                    }
                    localRepository.removePendingSync(sync.itemId)
                }
            }
        }

        return if (hasFailures) Result.retry() else Result.success()
    }

    companion object {
        private const val SYNC_WORK_NAME = "SyncPendingCaptures"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
        }
    }
}
