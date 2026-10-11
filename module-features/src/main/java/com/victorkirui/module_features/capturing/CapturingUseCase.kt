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
    private val notificationHelper: com.victorkirui.core.notification.NotificationHelper,
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
                            return CaptureResult.UpgradeRequired("You have reached the free tier limit of 10 AI extractions for this month. Upgrade to Remindly Pro for 250 monthly AI extractions.")

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
            
            localRepository.saveItem(initialItem)

            // 3. AI Extraction & Storage (Unified)
            val aiInput = AiInput(
                text = extractedTextFromOcr,
                url = sourceUrl,
                mediaUri = if (contentType == "DOCUMENT") null else mediaUri, // Rule: Do not upload raw PDF as image
                contentType = contentType,
                idempotencyKey = "${itemId}_${System.currentTimeMillis()}",
                source = sourceApp
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
                val cleanDeadline = result.deadline?.takeIf { 
                    it.isNotBlank() && !it.equals("null", ignoreCase = true) && !it.equals("none", ignoreCase = true) && !it.equals("n/a", ignoreCase = true)
                }
                val rawEventDate = result.eventDate?.takeIf { 
                    it.isNotBlank() && !it.equals("null", ignoreCase = true) && !it.equals("none", ignoreCase = true) && !it.equals("n/a", ignoreCase = true)
                }
                val cleanEventDate = DateUtils.sanitizeEventDate(rawEventDate, cleanDeadline)

                val updatedItem = initialItem.copy(
                    id = result.id ?: initialItem.id,
                    title = result.title,
                    summary = result.summary,
                    category = result.category,
                    deadline = cleanDeadline,
                    eventDate = cleanEventDate,
                    organization = result.organization,
                    checklist = result.checklist,
                    status = "READY",
                    metadata = mapOf(
                        "strategy" to result.strategy,
                        "analyzed_at" to timestampProvider()
                    )
                )

                localRepository.withTransaction {
                    // Remove temporary local item and any duplicate local item
                    localRepository.deleteItem(userId, initialItem.id)
                    val localDup = localRepository.findDuplicate(userId, sourceUrl, extractedTextFromOcr)
                    if (localDup != null && localDup.id != updatedItem.id) {
                        localRepository.deleteItem(userId, localDup.id)
                    }
                    localRepository.saveItem(updatedItem)
                    reminderScheduler.scheduleRemindersForItem(updatedItem)
                    localRepository.removePendingSync(initialItem.id)
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
                // Permanent AI failure (e.g. Quota Exceeded or Invalid Content)
                val isQuotaError = result.message.contains("limit", ignoreCase = true) || 
                                   result.message.contains("quota", ignoreCase = true) ||
                                   result.message.contains("exhausted", ignoreCase = true)

                val failedTitle = if (isQuotaError) "Monthly Limit Reached" else "Analysis Failed"
                val failedSummary = if (isQuotaError) 
                    "You've reached your monthly AI extraction limit. Upgrade to Remindly Pro for 250 monthly extractions."
                else 
                    result.message

                val failedItem = initialItem.copy(
                    title = failedTitle,
                    summary = failedSummary,
                    status = "FAILED"
                )
                localRepository.withTransaction {
                    localRepository.saveItem(failedItem)
                    localRepository.removePendingSync(itemId)
                }

                if (isQuotaError) {
                    notificationHelper.showQuotaExhaustedNotification()
                }

                CaptureResult.Error(failedSummary)
            } else {
                // AI Failed (transient/offline) but saved locally; schedule SyncWorker to analyze when online
                val retryingItem = initialItem.copy(
                    title = "Saved Offline",
                    summary = "Network unavailable. Remindly saved your capture locally and will analyze it automatically when online.",
                    status = "RETRYING"
                )
                localRepository.withTransaction {
                    localRepository.saveItem(retryingItem)
                    localRepository.addPendingSync(PendingSync(itemId = itemId))
                }
                SyncWorker.schedule(context)
                CaptureResult.SavedLocallyOnly(itemId, "Saved offline. Will analyze automatically when online.")
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
