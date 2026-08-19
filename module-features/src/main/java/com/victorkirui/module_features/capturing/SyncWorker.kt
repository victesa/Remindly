package com.victorkirui.module_features.capturing

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.*
import com.victorkirui.local.repository.LocalRepository
import com.victorkirui.module_features.capturing.ai.AiExtractor
import com.victorkirui.module_features.capturing.ai.AiInput
import com.victorkirui.module_features.capturing.ai.AiResult
import com.victorkirui.module_features.reminder.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class SyncWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val localRepository: LocalRepository by inject()
    private val aiExtractor: AiExtractor by inject()
    private val reminderScheduler: ReminderScheduler by inject()
    private val pdfTextExtractor: PdfTextExtractor by inject()

    override suspend fun doWork(): Result {
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

                val aiInput = AiInput(
                    text = extractedText ?: item.sourceUrl,
                    mediaUri = if (item.contentType == "DOCUMENT") null else mediaUri, // Rule: Do not upload raw PDF as image
                    contentType = item.contentType,
                    idempotencyKey = item.id // Reuse the same ID for retries of the same action
                )

                val result = aiExtractor.extract(aiInput)
                
                if (result is AiResult.Success) {
                    val updatedItem = item.copy(
                        title = result.title,
                        summary = result.summary,
                        category = result.category,
                        deadline = result.deadline,
                        eventDate = result.eventDate,
                        organization = result.organization,
                        status = "READY",
                        metadata = mapOf("strategy" to result.strategy, "synced" to "true")
                    )

                    localRepository.withTransaction {
                        localRepository.saveItem(updatedItem)
                        reminderScheduler.scheduleRemindersForItem(updatedItem)
                        localRepository.removePendingSync(item.id)
                    }
                } else {
                    hasFailures = true
                }
            } catch (e: Exception) {
                Log.e("SyncWorker", "Failed to sync item ${sync.itemId}", e)
                hasFailures = true
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
