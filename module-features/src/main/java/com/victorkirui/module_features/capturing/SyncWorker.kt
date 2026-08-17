package com.victorkirui.module_features.capturing

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingWorkPolicy
import com.victorkirui.core.model.CaptureMetadata
import com.victorkirui.core.model.CaptureRequest
import com.victorkirui.local.entity.Item
import com.victorkirui.local.repository.LocalRepository
import com.victorkirui.module_features.reminder.ReminderScheduler
import com.victorkirui.remote.CaptureApiService
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
    private val apiService: CaptureApiService by inject()
    private val reminderScheduler: ReminderScheduler by inject()
    private val pdfTextExtractor: PdfTextExtractor by inject()

    override suspend fun doWork(): Result {
        Log.e("SyncWorker", "Starting sync work. Checking for pending syncs...")
        
        val pendingSyncs = localRepository.getAllPendingSyncs()
        Log.e("SyncWorker", "Found ${pendingSyncs.size} pending syncs.")
        
        if (pendingSyncs.isEmpty()) {
            Log.e("SyncWorker", "No pending syncs found. Exiting.")
            return Result.success()
        }

        var hasFailures = false

        for (sync in pendingSyncs) {
            try {
                Log.e("SyncWorker", "Processing pending sync for itemId: ${sync.itemId}")
                val item = localRepository.getItem(sync.itemId)
                if (item == null) {
                    Log.e("SyncWorker", "Item ${sync.itemId} not found in database. Removing from pending sync.")
                    localRepository.removePendingSync(sync.itemId)
                    continue
                }

                val mediaUri = item.originalMediaUri?.let { Uri.parse(it) }
                Log.e("SyncWorker", "Item found: ${item.title}. mediaUri: $mediaUri")
                
                var extractedText = item.extractedText
                if (extractedText == null && mediaUri != null) {
                    if (item.contentType == "DOCUMENT" || item.originalMediaUri?.endsWith(".pdf", ignoreCase = true) == true) {
                        Log.e("SyncWorker", "Running missing OCR for PDF item: ${item.id}")
                        extractedText = pdfTextExtractor.extractText(mediaUri)
                    } else if (item.contentType == "IMAGE") {
                        Log.e("SyncWorker", "Running missing OCR for Image item: ${item.id}")
                        extractedText = pdfTextExtractor.extractTextFromImage(mediaUri)
                    }
                }

                val request = CaptureRequest(
                    itemId = item.id,
                    contentType = item.contentType,
                    extractedText = extractedText ?: item.summary,
                    capturedAt = item.createdAt,
                    metadata = CaptureMetadata(
                        source = "sync_worker",
                        timezone = java.util.TimeZone.getDefault().id
                    )
                )

                Log.e("SyncWorker", "Retrying sync for item: ${item.id}. Sending request...")
                val response = apiService.capture(request, mediaUri)
                Log.e("SyncWorker", "API call successful for item: ${item.id}")

                // Check for past dates
                val today = java.time.LocalDate.now()
                val deadlineDate = response.item.deadline?.let {
                    try { java.time.LocalDate.parse(it) } catch (e: Exception) { null }
                }
                val eventDate = response.item.eventDate?.let {
                    try { java.time.LocalDate.parse(it) } catch (e: Exception) { null }
                }

                if ((deadlineDate != null && deadlineDate.isBefore(today)) ||
                    (eventDate != null && eventDate.isBefore(today))) {
                    Log.w("SyncWorker", "Deleting synced item with past date: ${item.id}")
                    localRepository.withTransaction {
                        localRepository.deleteItem(item.id)
                        localRepository.removePendingSync(item.id)
                    }
                    continue
                }
                
                val updatedItem = Item(
                    id = item.id,
                    title = response.item.title,
                    summary = response.item.summary,
                    category = response.item.category,
                    deadline = response.item.deadline,
                    eventDate = response.item.eventDate,
                    organization = response.item.organization,
                    source = response.item.source ?: item.source,
                    sourceUrl = response.item.sourceUrl,
                    originalMediaUri = response.item.originalMediaUri ?: item.originalMediaUri,
                    extractedText = extractedText ?: item.extractedText,
                    contentType = item.contentType,
                    metadata = response.item.metadata.filterValues { it is String }.mapValues { it.value as String },
                    createdAt = response.item.createdAt,
                    status = response.item.state
                )

                localRepository.withTransaction {
                    localRepository.saveItem(updatedItem)
                    reminderScheduler.scheduleRemindersForItem(updatedItem)
                    localRepository.removePendingSync(item.id)
                }

                // Clean up local media file if it exists in internal storage
                item.originalMediaUri?.let { uriString ->
                    val uri = Uri.parse(uriString)
                    if (uri.scheme == "file" && uri.path?.contains(appContext.filesDir.path) == true) {
                        try {
                            val file = File(uri.path!!)
                            if (file.exists()) file.delete()
                        } catch (e: Exception) {
                            Log.w("SyncWorker", "Failed to delete local media file: ${e.message}")
                        }
                    }
                }
                Log.d("SyncWorker", "Sync successful for item: ${item.id}")
            } catch (e: Exception) {
                Log.e("SyncWorker", "Failed to sync item ${sync.itemId}: ${e.message}", e)
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
