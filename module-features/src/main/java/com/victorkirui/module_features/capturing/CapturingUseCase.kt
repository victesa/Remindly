package com.victorkirui.module_features.capturing

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.victorkirui.core.model.ShareContent
import com.victorkirui.local.entity.Item
import com.victorkirui.local.entity.PendingSync
import com.victorkirui.local.repository.LocalRepository
import com.victorkirui.module_features.capturing.ai.AiExtractor
import com.victorkirui.module_features.capturing.ai.AiInput
import com.victorkirui.module_features.capturing.ai.AiResult
import com.victorkirui.module_features.reminder.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.*

sealed class CaptureResult {
    data class Success(val itemId: String) : CaptureResult()
    data class Error(val message: String, val throwable: Throwable? = null) : CaptureResult()
    data class SavedLocallyOnly(val itemId: String, val message: String) : CaptureResult()
    data class Overdue(val item: Item) : CaptureResult()
}

class CapturingUseCase(
    private val context: Context,
    private val localRepository: LocalRepository,
    private val aiExtractor: AiExtractor,
    private val reminderScheduler: ReminderScheduler,
    private val pdfTextExtractor: PdfTextExtractor,
    private val timestampProvider: () -> String
) {
    suspend operator fun invoke(shareContent: ShareContent): CaptureResult {
        return try {
            val sourceApp = getResolvedSource(shareContent)
            val itemId = "local-" + UUID.randomUUID().toString()
            val capturedAt = timestampProvider()
            
            val sourceUrl = when (shareContent) {
                is ShareContent.Text -> extractUrl(shareContent.content)
                is ShareContent.Pdf -> shareContent.sourceUrl
                is ShareContent.Image -> shareContent.sourceUrl
                else -> null
            }

            var mediaUri: Uri? = null
            var extractedTextFromOcr: String? = null
            val contentType = getContentType(shareContent)

            // 1. Prepare Media and local OCR
            when (shareContent) {
                is ShareContent.Text -> {
                    val text = shareContent.content.trim()
                    if (text.isEmpty() && sourceUrl == null) {
                        return CaptureResult.Error("Captured text is empty.")
                    }
                    extractedTextFromOcr = text
                }
                is ShareContent.Image -> {
                    val originalUri = Uri.parse(shareContent.uriString)
                    mediaUri = copyToInternalStorage(originalUri, itemId, "IMAGE")
                    extractedTextFromOcr = pdfTextExtractor.extractTextFromImage(mediaUri ?: originalUri)
                }
                is ShareContent.Pdf -> {
                    val originalUri = Uri.parse(shareContent.uriString)
                    mediaUri = copyToInternalStorage(originalUri, itemId, "DOCUMENT")
                    extractedTextFromOcr = pdfTextExtractor.extractText(mediaUri ?: originalUri)
                }
                ShareContent.Unknown -> return CaptureResult.Error("Unknown content type")
            }

            if (mediaUri == null && extractedTextFromOcr == null && sourceUrl == null) {
                return CaptureResult.Error("Remindly cannot read this content. It may be restricted or empty.")
            }

            // 2. Save initial state to local DB (PENDING)
            val initialItem = Item(
                id = itemId,
                title = "Analysing...",
                summary = null,
                category = contentType,
                deadline = null,
                eventDate = null,
                source = sourceApp,
                sourceUrl = sourceUrl,
                originalMediaUri = mediaUri?.toString(),
                extractedText = extractedTextFromOcr,
                contentType = contentType,
                createdAt = capturedAt,
                status = "PENDING"
            )
            
            localRepository.withTransaction {
                localRepository.saveItem(initialItem)
                localRepository.addPendingSync(PendingSync(itemId = itemId))
            }

            // 3. AI Extraction
            val aiInput = AiInput(
                text = extractedTextFromOcr ?: sourceUrl,
                mediaUri = if (contentType == "DOCUMENT") null else mediaUri, // Rule: Do not upload raw PDF as image
                contentType = contentType,
                idempotencyKey = itemId // Use the itemId (generated once per action) as Idempotency-Key
            )

            val result = aiExtractor.extract(aiInput)
            
            if (result is AiResult.Success) {
                // Check for past dates
                val today = java.time.LocalDate.now()
                val deadlineDate = result.deadline?.let { try { java.time.LocalDate.parse(it) } catch (e: Exception) { null } }
                val eventDate = result.eventDate?.let { try { java.time.LocalDate.parse(it) } catch (e: Exception) { null } }

                if ((deadlineDate != null && deadlineDate.isBefore(today)) || (eventDate != null && eventDate.isBefore(today))) {
                    localRepository.withTransaction {
                        localRepository.deleteItem(itemId)
                        localRepository.removePendingSync(itemId)
                    }
                    return CaptureResult.Overdue(Item(
                        id = itemId, title = result.title, deadline = result.deadline, eventDate = result.eventDate, status = "REJECTED", createdAt = capturedAt, summary = result.summary, category = result.category
                    ))
                }

                val updatedItem = initialItem.copy(
                    title = result.title,
                    summary = result.summary,
                    category = result.category,
                    deadline = result.deadline,
                    eventDate = result.eventDate,
                    organization = result.organization,
                    status = "READY",
                    metadata = mapOf("strategy" to result.strategy)
                )

                localRepository.withTransaction {
                    localRepository.saveItem(updatedItem)
                    reminderScheduler.scheduleRemindersForItem(updatedItem)
                    localRepository.removePendingSync(itemId)
                }
                CaptureResult.Success(itemId)
            } else {
                // AI Failed but we have it locally, schedule SyncWorker to retry AI later
                SyncWorker.schedule(context)
                CaptureResult.SavedLocallyOnly(itemId, "AI Analysis is busy. Saved offline and will retry shortly.")
            }
        } catch (e: Exception) {
            Log.e("CapturingUseCase", "Capture flow failed", e)
            CaptureResult.Error(e.message ?: "An unexpected error occurred")
        }
    }

    private fun getResolvedSource(shareContent: ShareContent): String {
        val raw = when (shareContent) {
            is ShareContent.Text -> shareContent.source
            is ShareContent.Image -> shareContent.source
            is ShareContent.Pdf -> shareContent.source
            else -> null
        } ?: "intent"
        
        return if (raw.contains(".")) getAppNameFromPackage(raw) else raw
    }

    private fun getContentType(shareContent: ShareContent): String = when (shareContent) {
        is ShareContent.Text -> "TEXT"
        is ShareContent.Image -> "IMAGE"
        is ShareContent.Pdf -> "DOCUMENT"
        else -> "OTHER"
    }

    private fun getAppNameFromPackage(packageName: String): String {
        return try {
            val pm = context.packageManager
            val ai = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(ai).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun copyToInternalStorage(uri: Uri, itemId: String, contentType: String): Uri? {
        Log.d("CapturingUseCase", "Copying $uri to internal storage. itemId: $itemId")
        return try {
            val detectedMime = if (uri.scheme == "content") context.contentResolver.getType(uri) else null
            var extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(detectedMime)
            if (extension == null) {
                extension = when (contentType) {
                    "DOCUMENT" -> "pdf"
                    "IMAGE" -> "jpg"
                    else -> null
                }
            }
            val destDir = File(context.filesDir, "captures").apply { if (!exists()) mkdirs() }
            val file = File(destDir, "captured_${itemId}${if (extension != null) ".$extension" else ""}")
            
            val inputStream = if (uri.scheme == "file") {
                File(uri.path!!).inputStream()
            } else {
                context.contentResolver.openInputStream(uri)
            }

            inputStream?.use { input -> 
                FileOutputStream(file).use { output -> 
                    input.copyTo(output) 
                } 
            } ?: throw Exception("Could not open input stream")

            val resultUri = Uri.fromFile(file)
            Log.d("CapturingUseCase", "Successfully copied to: $resultUri")
            resultUri
        } catch (e: Exception) {
            Log.e("CapturingUseCase", "Internal storage copy failed", e)
            null
        }
    }

    private fun extractUrl(text: String): String? {
        val urlRegex = "(https?://[\\w\\d\\-._~:/?#\\[\\]@!$&'()*+,;=]+)".toRegex()
        return urlRegex.find(text)?.value
    }
}
