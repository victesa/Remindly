package com.victorkirui.module_features.capturing

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.victorkirui.core.model.ShareContent
import com.victorkirui.core.repository.AuthRepository
import com.victorkirui.core.repository.SubscriptionRepository
import com.victorkirui.core.repository.SubscriptionTier
import com.victorkirui.core.util.DateUtils
import com.victorkirui.local.entity.Item
import com.victorkirui.local.entity.PendingSync
import com.victorkirui.local.repository.LocalRepository
import com.victorkirui.module_features.capturing.ai.AiExtractor
import com.victorkirui.module_features.capturing.ai.AiInput
import com.victorkirui.module_features.capturing.ai.AiResult
import com.victorkirui.module_features.reminder.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.*

sealed class CaptureResult {
    data class Success(val itemId: String) : CaptureResult()
    data class Error(val message: String, val throwable: Throwable? = null) : CaptureResult()
    data class SavedLocallyOnly(val itemId: String, val message: String) : CaptureResult()
    data class Overdue(val item: Item) : CaptureResult()
    data class UpgradeRequired(val message: String) : CaptureResult()
    data class Duplicate(val existingItemId: String) : CaptureResult()
}

class CapturingUseCase(
    private val context: Context,
    private val authRepository: AuthRepository,
    private val localRepository: LocalRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val aiExtractor: AiExtractor,
    private val reminderScheduler: ReminderScheduler,
    private val calendarManager: com.victorkirui.core.calendar.CalendarManager,
    private val pdfTextExtractor: PdfTextExtractor,
    private val timestampProvider: () -> String
) {
    suspend operator fun invoke(shareContent: ShareContent): CaptureResult {
        val userId = authRepository.currentUser.value?.uid ?: return CaptureResult.Error("User not logged in")
        val tier = subscriptionRepository.subscriptionTier.first()
        
        val itemId = "local-" + UUID.randomUUID().toString()
        val capturedAt = timestampProvider()
        val sourceApp = getResolvedSource(shareContent)
        val contentType = getContentType(shareContent)

        return try {
            val sourceUrl = when (shareContent) {
                is ShareContent.Text -> extractUrl(shareContent.content)
                is ShareContent.Pdf -> shareContent.sourceUrl
                is ShareContent.Image -> shareContent.sourceUrl
                else -> null
            }

            var mediaUri: Uri? = null
            var extractedTextFromOcr: String?

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
                    if (tier == SubscriptionTier.FREE) {
                        val currentCount = subscriptionRepository.imageCaptureCount.first()
                        if (currentCount >= subscriptionRepository.freeImageLimit) {
                            return CaptureResult.UpgradeRequired("You have reached the limit of image captures for the free tier. Upgrade to Premium for unlimited visual analysis.")
                        }
                        subscriptionRepository.incrementImageCaptureCount()
                    }
                    
                    val originalUri = Uri.parse(shareContent.uriString)
                    mediaUri = copyToInternalStorage(originalUri, itemId, "IMAGE")
                    extractedTextFromOcr = pdfTextExtractor.extractTextFromImage(mediaUri ?: originalUri)
                }
                is ShareContent.Pdf -> {
                    val originalUri = Uri.parse(shareContent.uriString)
                    mediaUri = copyToInternalStorage(originalUri, itemId, "DOCUMENT")
                    
                    val maxPages = if (tier == SubscriptionTier.PREMIUM) 
                        subscriptionRepository.premiumPdfPageLimit 
                    else 
                        subscriptionRepository.freePdfPageLimit
                        
                    Log.d("CapturingUseCase", "Processing PDF. Tier: $tier, maxPages: $maxPages")
                    extractedTextFromOcr = pdfTextExtractor.extractText(mediaUri ?: originalUri, maxPages = maxPages)
                }
                ShareContent.Unknown -> return CaptureResult.Error("Unknown content type")
            }

            // 1.5 Deduplication Check
            val duplicate = localRepository.findDuplicate(userId, sourceUrl, extractedTextFromOcr)
            if (duplicate != null) {
                Log.d("CapturingUseCase", "Duplicate found for item: ${duplicate.id}")
                return CaptureResult.Duplicate(duplicate.id)
            }

            if (mediaUri == null && extractedTextFromOcr == null && sourceUrl == null) {
                val failedItem = Item(
                    id = itemId,
                    userId = userId,
                    title = "Analysis Failed",
                    summary = "Remindly cannot read this content. It may be restricted or empty.",
                    category = contentType,
                    deadline = null,
                    eventDate = null,
                    source = sourceApp,
                    sourceUrl = sourceUrl,
                    originalMediaUri = null,
                    extractedText = null,
                    contentType = contentType,
                    createdAt = capturedAt,
                    status = "FAILED"
                )
                localRepository.saveItem(failedItem)
                return CaptureResult.Error("Remindly cannot read this content. It may be restricted or empty.")
            }

            // 2. Save initial state to local DB (PENDING)
            val initialItem = Item(
                id = itemId,
                userId = userId,
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

            // 3. AI Extraction & Storage (Unified)
            val aiInput = AiInput(
                text = extractedTextFromOcr,
                url = sourceUrl,
                mediaUri = if (contentType == "DOCUMENT") null else mediaUri, // Rule: Do not upload raw PDF as image
                contentType = contentType,
                idempotencyKey = itemId 
            )

            // CRITICAL: Double-check that we are actually sending content to the backend.
            if (aiInput.text == null && aiInput.url == null && aiInput.mediaUri == null) {
                val errorMsg = if (contentType == "DOCUMENT") 
                    "Could not extract any text from this PDF. It might be scanned or restricted."
                else 
                    "No readable content found to analyze."
                
                return CaptureResult.Error(errorMsg)
            }

            // This single call to aiExtractor handles extraction AND storage on the backend
            val result = aiExtractor.extract(aiInput)
            
            // CRITICAL: Check if item still exists before saving results. 
            // The user might have deleted it while AI was processing.
            val currentItem = localRepository.getItem(itemId)
            if (currentItem == null) {
                Log.d("CapturingUseCase", "Item $itemId was deleted while processing. Aborting save.")
                return CaptureResult.Error("Item was deleted during analysis.")
            }

            if (result is AiResult.Success) {
                // Precision check for past deadlines
                val deadlineDateTime = result.deadline?.let { DateUtils.parseToDateTime(it) }
                val eventDateTime = result.eventDate?.let { DateUtils.parseToDateTime(it) }
                val now = java.time.LocalDateTime.now()

                // Reject if the deadline or event date is in the past
                val rejectionThreshold = now

                if ((deadlineDateTime != null && deadlineDateTime.isBefore(rejectionThreshold)) || 
                    (eventDateTime != null && eventDateTime.isBefore(rejectionThreshold))) {
                    
                    localRepository.withTransaction {
                        localRepository.deleteItem(userId, itemId)
                        localRepository.removePendingSync(itemId)
                    }
                    return CaptureResult.Overdue(Item(
                        id = itemId, 
                        userId = userId, 
                        title = result.title, 
                        deadline = result.deadline, 
                        eventDate = result.eventDate, 
                        status = "REJECTED", 
                        createdAt = capturedAt, 
                        summary = result.summary, 
                        category = result.category
                    ))
                }

                val updatedItem = initialItem.copy(
                    id = result.id ?: initialItem.id,
                    title = result.title,
                    summary = result.summary,
                    category = result.category,
                    deadline = result.deadline,
                    eventDate = result.eventDate,
                    organization = result.organization,
                    checklist = result.checklist,
                    status = "READY",
                    metadata = mapOf(
                        "strategy" to result.strategy,
                        "analyzed_at" to timestampProvider()
                    )
                )

                localRepository.withTransaction {
                    if (updatedItem.id != initialItem.id) {
                        localRepository.deleteItem(userId, initialItem.id)
                    }
                    localRepository.saveItem(updatedItem)
                    reminderScheduler.scheduleRemindersForItem(updatedItem)
                    localRepository.removePendingSync(itemId)
                }

                // AUTO-SYNC TO CALENDAR (Premium Only)
                if (tier == SubscriptionTier.PREMIUM && subscriptionRepository.isCalendarSyncEnabled.first()) {
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
                            Log.e("CapturingUseCase", "Auto-sync failed", e)
                        }
                    }
                }

                CaptureResult.Success(itemId)
            } else if (result is AiResult.Error && !result.isTransient) {
                // Permanent AI failure
                val failedItem = initialItem.copy(
                    title = "Analysis Failed",
                    summary = result.message,
                    status = "FAILED"
                )
                localRepository.withTransaction {
                    localRepository.saveItem(failedItem)
                    localRepository.removePendingSync(itemId)
                }
                CaptureResult.Error(result.message)
            } else {
                // AI Failed (transient) but we have it locally, schedule SyncWorker to retry AI later
                val retryingItem = initialItem.copy(status = "RETRYING")
                localRepository.saveItem(retryingItem)
                SyncWorker.schedule(context)
                CaptureResult.SavedLocallyOnly(itemId, "AI Analysis is busy. Saved offline and will retry shortly.")
            }
        } catch (e: Exception) {
            Log.e("CapturingUseCase", "Capture flow failed", e)
            try {
                // Try to mark as FAILED if it was saved
                localRepository.getItem(itemId)?.let {
                    localRepository.saveItem(it.copy(status = "FAILED", title = "Analysis Error"))
                }
            } catch (ex: Exception) { /* ignore */ }
            CaptureResult.Error(e.message ?: "An unexpected error occurred")
        }
    }

    private fun getResolvedSource(shareContent: ShareContent): String {
        val raw = when (shareContent) {
            is ShareContent.Text -> shareContent.source
            is ShareContent.Image -> shareContent.source
            is ShareContent.Pdf -> shareContent.source
            else -> null
        }
        
        val url = when (shareContent) {
            is ShareContent.Text -> extractUrl(shareContent.content)
            is ShareContent.Pdf -> shareContent.sourceUrl
            is ShareContent.Image -> shareContent.sourceUrl
            else -> null
        }

        val resolvedFromUrl = url?.let { inferSourceFromUrl(it) }
        if (resolvedFromUrl != null) return resolvedFromUrl

        if (raw == null || raw == "Unknown" || raw == "intent") return "Other"
        
        return if (raw.contains(".")) getAppNameFromPackage(raw) else raw
    }

    private fun inferSourceFromUrl(url: String): String? {
        val lowerUrl = url.lowercase()
        return when {
            lowerUrl.contains("linkedin.com") -> "LinkedIn"
            lowerUrl.contains("gmail.com") || lowerUrl.contains("mail.google.com") -> "Gmail"
            lowerUrl.contains("drive.google.com") -> "Google Drive"
            lowerUrl.contains("docs.google.com") -> "Google Docs"
            lowerUrl.contains("github.com") -> "GitHub"
            lowerUrl.contains("twitter.com") || lowerUrl.contains("x.com") -> "X (Twitter)"
            lowerUrl.contains("facebook.com") -> "Facebook"
            lowerUrl.contains("instagram.com") -> "Instagram"
            lowerUrl.contains("youtube.com") || lowerUrl.contains("youtu.be") -> "YouTube"
            lowerUrl.contains("whatsapp.com") -> "WhatsApp"
            lowerUrl.contains("slack.com") -> "Slack"
            lowerUrl.contains("notion.so") -> "Notion"
            lowerUrl.contains("amazon.com") || lowerUrl.contains("amazon.co") -> "Amazon"
            else -> null
        }
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
