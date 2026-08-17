package com.victorkirui.module_features.capturing

import android.util.Log
import android.net.Uri
import com.victorkirui.core.model.CaptureMetadata
import com.victorkirui.core.model.CaptureRequest
import com.victorkirui.core.model.ShareContent
import com.victorkirui.local.entity.Item
import com.victorkirui.local.entity.PendingSync
import com.victorkirui.local.repository.LocalRepository
import com.victorkirui.module_features.reminder.ReminderScheduler
import com.victorkirui.remote.CaptureApiService
import android.content.Context
import java.util.UUID
import java.io.File
import java.io.FileOutputStream
import android.webkit.MimeTypeMap
import androidx.core.net.toFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class CaptureResult {
    object Success : CaptureResult()
    data class Error(val message: String, val throwable: Throwable? = null) : CaptureResult()
    object SavedLocallyOnly : CaptureResult()
    data class Overdue(val item: Item) : CaptureResult()
}

class CapturingUseCase(
    private val context: Context,
    private val localRepository: LocalRepository,
    private val apiService: CaptureApiService,
    private val reminderScheduler: ReminderScheduler,
    private val pdfTextExtractor: PdfTextExtractor,
    private val timestampProvider: () -> String
) {
    suspend operator fun invoke(shareContent: ShareContent): CaptureResult {
        return try {
            val rawSource = when (shareContent) {
                is ShareContent.Text -> shareContent.source
                is ShareContent.Image -> shareContent.source
                is ShareContent.Pdf -> shareContent.source
                ShareContent.Unknown -> null
            } ?: "intent"
            
            val sourceApp = if (rawSource.contains(".")) {
                getAppNameFromPackage(rawSource)
            } else {
                rawSource
            }
            Log.d("CapturingUseCase", "Captured source: raw='$rawSource', resolved='$sourceApp'")

            val itemId = "local-" + UUID.randomUUID().toString()
            val capturedAt = timestampProvider()
            val metadata = CaptureMetadata(
                source = sourceApp,
                timezone = java.util.TimeZone.getDefault().id
            )

            var mediaUri: Uri? = null
            var extractedTextFromOcr: String? = null
            
            val request = when (shareContent) {
                is ShareContent.Text -> CaptureRequest(
                    itemId = itemId,
                    contentType = "TEXT",
                    extractedText = shareContent.content,
                    capturedAt = capturedAt,
                    metadata = metadata
                )
                is ShareContent.Image -> {
                    mediaUri = copyToInternalStorage(Uri.parse(shareContent.uriString), itemId)
                    // Optional: Run OCR on images too if backend requires it for consistency
                    // extractedTextFromOcr = mediaUri?.let { pdfTextExtractor.extractTextFromImage(it) }
                    CaptureRequest(
                        itemId = itemId,
                        contentType = "IMAGE",
                        extractedText = null,
                        capturedAt = capturedAt,
                        metadata = metadata
                    )
                }
                is ShareContent.Pdf -> {
                    val originalUri = Uri.parse(shareContent.uriString)
                    mediaUri = copyToInternalStorage(originalUri, itemId)
                    
                    // Run OCR on PDF
                    extractedTextFromOcr = mediaUri?.let { pdfTextExtractor.extractText(it) }
                    
                    CaptureRequest(
                        itemId = itemId,
                        contentType = "DOCUMENT",
                        extractedText = extractedTextFromOcr,
                        capturedAt = capturedAt,
                        metadata = metadata
                    )
                }
                ShareContent.Unknown -> return CaptureResult.Error("Unknown content type")
            }

            // 1. Save initial state to local DB (PENDING) in a transaction
            val initialItem = Item(
                id = itemId,
                title = "Processing...",
                summary = null,
                category = null,
                deadline = null,
                eventDate = null,
                originalMediaUri = mediaUri?.toString(),
                extractedText = request.extractedText,
                contentType = request.contentType,
                createdAt = capturedAt,
                status = "PENDING"
            )
            
            localRepository.withTransaction {
                localRepository.saveItem(initialItem)
                localRepository.addPendingSync(PendingSync(itemId = itemId))
            }

            // 2. Send to remote
            val response = try {
                Log.d("CapturingUseCase", "Sending request to remote: $request, mediaUri: $mediaUri")
                apiService.capture(request, mediaUri)
            } catch (e: Exception) {
                Log.e("CapturingUseCase", "Remote capture failed: ${e.message}", e)
                // If remote fails, we've already saved locally
                SyncWorker.schedule(context)
                return CaptureResult.SavedLocallyOnly
            }

            Log.d("CapturingUseCase", "Remote capture successful: $response")

            // 3. Check if overdue BEFORE saving to DB
            val today = java.time.LocalDate.now()
            val deadlineDate = response.item.deadline?.let {
                try { java.time.LocalDate.parse(it) } catch (e: Exception) { null }
            }
            val eventDate = response.item.eventDate?.let {
                try { java.time.LocalDate.parse(it) } catch (e: Exception) { null }
            }

            if ((deadlineDate != null && deadlineDate.isBefore(today)) ||
                (eventDate != null && eventDate.isBefore(today))) {
                
                Log.w("CapturingUseCase", "Rejecting capture with past date: deadline=$deadlineDate, event=$eventDate")
                
                // Cleanup: Remove the pending item we created at the start
                localRepository.withTransaction {
                    localRepository.deleteItem(itemId)
                    localRepository.removePendingSync(itemId)
                }
                
                return CaptureResult.Overdue(Item(
                    id = itemId,
                    title = response.item.title,
                    summary = response.item.summary,
                    category = response.item.category,
                    deadline = response.item.deadline,
                    eventDate = response.item.eventDate,
                    status = "REJECTED",
                    createdAt = capturedAt
                ))
            }

            // 4. Update local DB with response & 5. Schedule Reminders in a transaction
            val updatedItem = Item(
                id = itemId,
                title = response.item.title,
                summary = response.item.summary,
                category = response.item.category,
                deadline = response.item.deadline,
                eventDate = response.item.eventDate,
                organization = response.item.organization,
                source = response.item.source ?: sourceApp,
                sourceUrl = response.item.sourceUrl,
                originalMediaUri = response.item.originalMediaUri ?: mediaUri?.toString(),
                extractedText = initialItem.extractedText,
                contentType = initialItem.contentType,
                metadata = response.item.metadata.filterValues { it is String }.mapValues { it.value as String },
                createdAt = response.item.createdAt,
                status = response.item.state
            )
            
            localRepository.withTransaction {
                localRepository.saveItem(updatedItem)
                // 5. Schedule Reminders
                reminderScheduler.scheduleRemindersForItem(updatedItem)
                // 6. Remote was successful, remove from pending sync
                localRepository.removePendingSync(itemId)
            }

            CaptureResult.Success
        } catch (e: Exception) {
            CaptureResult.Error(e.message ?: "An unexpected error occurred", e)
        }
    }

    private fun getAppNameFromPackage(packageName: String): String {
        return try {
            val pm = context.packageManager
            val ai = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, android.content.pm.PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(ai).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun copyToInternalStorage(uri: Uri, itemId: String): Uri? {
        Log.d("CapturingUseCase", "Copying URI to internal storage: $uri")
        return try {
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(context.contentResolver.getType(uri))
            val fileName = "captured_${itemId}${if (extension != null) ".$extension" else ""}"
            val destFile = File(context.filesDir, "captures").apply { if (!exists()) mkdirs() }
            val file = File(destFile, fileName)
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            val resultUri = Uri.fromFile(file)
            Log.d("CapturingUseCase", "Successfully copied to: $resultUri")
            resultUri
        } catch (e: Exception) {
            Log.e("CapturingUseCase", "Failed to copy media to internal storage: ${e.message}", e)
            null
        }
    }
}
