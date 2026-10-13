package com.victorkirui.module_features.capturing

import android.content.Context
import android.util.Log
import androidx.work.*
import com.victorkirui.core.repository.AuthRepository
import com.victorkirui.core.util.DateUtils
import com.victorkirui.local.entity.Item
import com.victorkirui.local.repository.LocalRepository
import com.victorkirui.remote.CaptureApiService
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import retrofit2.HttpException
import java.io.IOException

class SyncDownWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val localRepository: LocalRepository by inject()
    private val captureApiService: CaptureApiService by inject()
    private val authRepository: AuthRepository by inject()
    private val subscriptionRepository: com.victorkirui.core.repository.SubscriptionRepository by inject()
    private val reminderScheduler: com.victorkirui.module_features.reminder.ReminderScheduler by inject()

    override suspend fun doWork(): Result {
        val attempt = runAttemptCount
        Log.d(TAG, "Starting sync-down attempt #$attempt...")

        val userId = authRepository.currentUser.value?.uid 
            ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

        if (userId.isNullOrEmpty()) {
            Log.d(TAG, "No active user logged in. Canceling sync-down.")
            return Result.failure()
        }

        // Strictly verify entitlement via backend profile GET /v1/me endpoint ONLY.
        // JWT custom claims are NOT used for data fetching authorization.
        var isBackendConfirmedPro = false
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser

        try {
            val profile = captureApiService.getMe()
            Log.d(TAG, "GET /v1/me response: success=${profile.success}, isProUser=${profile.isProUser}, tier=${profile.tier}, quotaLimit=${profile.quotaLimit}, message=${profile.message}")
            if (profile.success && profile.isProUser) {
                isBackendConfirmedPro = true
                if (profile.quotaLimit >= 0 && profile.quotaRemaining >= 0) {
                    subscriptionRepository.updateQuota(profile.quotaLimit, profile.quotaRemaining)
                }
                // Sync user display name if empty or missing locally
                val backendName = profile.user?.name
                if (!backendName.isNullOrBlank() && user != null && user.displayName.isNullOrBlank()) {
                    Log.d(TAG, "Syncing backend display name to Firebase Auth: $backendName")
                    authRepository.updateDisplayName(backendName)
                }
            } else {
                Log.w(TAG, "GET /v1/me did NOT confirm pro status (success=${profile.success}, isProUser=${profile.isProUser})")
            }
        } catch (e: Exception) {
            Log.w(TAG, "GET /v1/me check exception in SyncDownWorker: [${e.javaClass.name}] ${e.message}", e)
        }

        // STRICT ENTITLEMENT GUARD: Abort fetching immediately if backend profile did not confirm active Pro status
        val targetTier = if (isBackendConfirmedPro) com.victorkirui.core.repository.SubscriptionTier.PREMIUM else com.victorkirui.core.repository.SubscriptionTier.FREE
        val currentTier = subscriptionRepository.subscriptionTier.first()

        if (targetTier != currentTier) {
            Log.i(TAG, "Updating subscription tier in DataStore: $currentTier -> $targetTier")
            subscriptionRepository.updateTier(targetTier)
        }

        if (!isBackendConfirmedPro) {
            Log.d(TAG, "Backend GET /v1/me did NOT confirm active Pro status. Explicitly aborting cloud data restoration.")
            subscriptionRepository.setInitialSyncPending(false)
            return Result.success()
        }

        return try {
            val localCount = localRepository.getAllItemsSyncForUser(userId).size
            val storedLastSyncedAt = subscriptionRepository.lastSyncedAt.first()
            val lastSyncedAt = if (localCount == 0) null else storedLastSyncedAt

            val remoteItems: List<com.victorkirui.core.model.StoredReminderItem>
            val deletedIds: List<String>
            val newSyncTimestamp: String?

            if (lastSyncedAt.isNullOrBlank()) {
                Log.d(TAG, "No valid lastSyncedAt checkpoint or empty local DB for user $userId (Initial Sync). Calling GET /v1/items/sync without since parameter...")
                val deltaResponse = try {
                    captureApiService.getSyncDeltas(null)
                } catch (e: Exception) {
                    Log.w(TAG, "getSyncDeltas(null) failed: ${e.message}. Falling back to getCaptures().")
                    null
                }

                remoteItems = deltaResponse?.updatedItems ?: captureApiService.getCaptures(limit = null)
                deletedIds = deltaResponse?.deletedItemIds ?: emptyList()
                newSyncTimestamp = deltaResponse?.syncTimestamp ?: if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    java.time.Instant.now().toString()
                } else {
                    java.util.Date().toString()
                }
            } else {

                Log.d(TAG, "Syncing down deltas for user $userId (since='$lastSyncedAt')...")
                val deltaResponse = captureApiService.getSyncDeltas(lastSyncedAt)
                remoteItems = deltaResponse.updatedItems ?: emptyList()
                deletedIds = deltaResponse.deletedItemIds ?: emptyList()
                newSyncTimestamp = deltaResponse.syncTimestamp ?: if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    java.time.Instant.now().toString()
                } else {
                    java.util.Date().toString()
                }
            }

            Log.d(TAG, "Fetched sync response: ${remoteItems.size} items, ${deletedIds.size} deleted items.")

            localRepository.withTransaction {
                // 1. Process Deleted Items (Tombstones)
                deletedIds.forEach { deletedId ->
                    if (deletedId.isNotBlank()) {
                        Log.d(TAG, "Deleting tombstone item $deletedId from local DB...")
                        localRepository.deleteItem(userId, deletedId)
                    }
                }

                // 2. Process Updated/New Items
                remoteItems.forEach { remote ->
                    val payload = remote.data ?: com.victorkirui.core.model.ExtractedReminderPayload()
                    val title = remote.title?.ifBlank { null }
                        ?: payload.title.takeIf { it.isNotBlank() }
                        ?: "Untitled"
                    val summary = remote.summary ?: payload.summary
                    val category = remote.category?.ifBlank { null }
                        ?: payload.category.takeIf { it.isNotBlank() }
                        ?: "OTHER"
                    val rawDeadline = remote.deadline ?: payload.deadline
                    val deadline = rawDeadline?.takeIf { 
                        it.isNotBlank() && !it.equals("null", ignoreCase = true) && !it.equals("none", ignoreCase = true) && !it.equals("n/a", ignoreCase = true) 
                    }
                    val rawEventDate = remote.eventDate ?: payload.eventDate
                    val cleanEventDate = rawEventDate?.takeIf { 
                        it.isNotBlank() && !it.equals("null", ignoreCase = true) && !it.equals("none", ignoreCase = true) && !it.equals("n/a", ignoreCase = true) 
                    }
                    val eventDate = DateUtils.sanitizeEventDate(cleanEventDate, deadline)
                    val organization = remote.organization ?: payload.organization
                    val url = remote.sourceUrl ?: remote.url ?: payload.url
                    val strategy = (remote.metadata?.get("strategy") as? String) ?: payload.strategy
                    val tierValue = (remote.metadata?.get("tier") as? String) ?: payload.tier
                    val actionableItems = remote.checklist ?: payload.actionableItems
                    val status = remote.state ?: remote.status ?: "READY"
                    val createdAt = remote.createdAt
                        ?: remote.extractedAt
                        ?: (remote.metadata?.get("extracted_at") as? String)
                        ?: java.time.Instant.now().toString()

                    val remoteId = if (remote.id.isNotBlank()) remote.id else java.util.UUID.randomUUID().toString()

                    // Check if a local item exists for this same content and remove old copy to avoid duplicate entries
                    val existingLocal = if (!url.isNullOrBlank() || !remote.inputSnippet.isNullOrBlank() || title.isNotBlank()) {
                        localRepository.findDuplicateEx(userId = userId, url = url, text = remote.inputSnippet, title = title)
                    } else null

                    if (existingLocal != null && existingLocal.id != remoteId) {
                        Log.d(TAG, "Deduplicating: Deleting old local item ${existingLocal.id} (${existingLocal.title}) in favor of cloud item $remoteId")
                        localRepository.deleteItem(userId, existingLocal.id)
                    }

                    val rawSourceFromRemote = run {
                        var src: String? = null
                        if (!remote.source.isNullOrBlank() && 
                            !remote.source.equals("firestore", ignoreCase = true) && 
                            !remote.source.equals("firebase", ignoreCase = true)) {
                            src = remote.source
                        }
                        if (src.isNullOrBlank()) {
                            val metaSrc = (remote.metadata?.get("source") as? String)
                                ?: (remote.metadata?.get("clientSource") as? String)
                                ?: (remote.metadata?.get("client_source") as? String)
                            if (!metaSrc.isNullOrBlank() && 
                                !metaSrc.equals("firestore", ignoreCase = true) && 
                                !metaSrc.equals("firebase", ignoreCase = true)) {
                                src = metaSrc
                            }
                        }
                        if (src.isNullOrBlank() && !remote.persistedSource.isNullOrBlank()) {
                            if (!remote.persistedSource.equals("firestore", ignoreCase = true) && 
                                !remote.persistedSource.equals("firebase", ignoreCase = true)) {
                                src = remote.persistedSource
                            }
                        }
                        src
                    }

                    val resolvedSource = com.victorkirui.core.ShareIntentHandler().resolveSource(
                        rawSource = rawSourceFromRemote,
                        url = url,
                        type = remote.sourceType
                    )

                    val remoteMetadata = (remote.metadata ?: emptyMap()).mapValues { it.value?.toString() ?: "" }
                    val combinedMetadata = mutableMapOf<String, String>()
                    combinedMetadata.putAll(remoteMetadata.filterValues { it.isNotEmpty() })
                    combinedMetadata["strategy"] = strategy
                    combinedMetadata["tier"] = tierValue
                    combinedMetadata["source_type"] = remote.sourceType ?: ""
                    combinedMetadata["extracted_at"] = createdAt

                    val localItem = Item(
                        id = remoteId,
                        userId = userId,
                        title = title,
                        summary = summary,
                        category = category,
                        deadline = deadline,
                        eventDate = eventDate,
                        organization = organization,
                        source = resolvedSource,
                        sourceUrl = url,
                        originalMediaUri = null,
                        metadata = combinedMetadata.filterValues { it.isNotEmpty() },
                        extractedText = remote.inputSnippet,
                        contentType = when(remote.sourceType) {
                            "image" -> "IMAGE"
                            "text" -> "TEXT"
                            "url" -> "TEXT"
                            else -> "OTHER"
                        },
                        checklist = actionableItems,
                        status = if (status.equals("DONE", ignoreCase = true) || status.equals("completed", ignoreCase = true)) "DONE" else "READY",
                        createdAt = createdAt
                    )
                    localRepository.saveItem(localItem)
                    Log.d(TAG, "Saved cloud item locally: id='$remoteId', title='$title', category='$category', status='${localItem.status}', userId='$userId'")
                    reminderScheduler.scheduleRemindersForItem(localItem)
                }

                // Deduplicate any existing duplicate items in local database by title / URL / text snippet
                val allLocalSyncItems = localRepository.getAllItemsSyncForUser(userId)
                val groupedByContent = allLocalSyncItems.groupBy { 
                    val t = it.title.trim().lowercase()
                    val u = it.sourceUrl?.trim()?.lowercase() ?: ""
                    val s = it.extractedText?.trim()?.lowercase() ?: ""
                    if (u.isNotBlank()) "url:$u" else if (s.isNotBlank() && s.length >= 15) "text:${s.take(50)}" else "title:$t"
                }

                groupedByContent.forEach { (key, duplicates) ->
                    if (duplicates.size > 1 && key.isNotBlank()) {
                        val sorted = duplicates.sortedWith(
                            compareByDescending<Item> { !it.id.startsWith("local-") }
                                .thenByDescending { it.status == "DONE" }
                                .thenByDescending { it.createdAt }
                        )
                        val keep = sorted.first()
                        val toDelete = sorted.drop(1)
                        
                        toDelete.forEach { duplicateItem ->
                            Log.d(TAG, "Sync-down cleanup: Deleting duplicate item ${duplicateItem.id} (${duplicateItem.title}) in favor of ${keep.id}")
                            localRepository.deleteItem(userId, duplicateItem.id)
                        }
                    }
                }
            }

            // Save the new sync timestamp for future delta requests
            if (!newSyncTimestamp.isNullOrBlank()) {
                subscriptionRepository.updateLastSyncedAt(newSyncTimestamp)
                Log.d(TAG, "Updated lastSyncedAt checkpoint to '$newSyncTimestamp'")
            }

            Log.d(TAG, "Sync-down complete. ${remoteItems.size} items updated, ${deletedIds.size} deleted.")
            Result.success()
        } catch (e: HttpException) {
            val code = e.code()
            val errorBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
            Log.e(TAG, "HTTP $code error during sync-down. Response: $errorBody", e)
            if (code == 403 || code == 401) {
                Log.w(TAG, "HTTP $code (Subscription Expired / Forbidden) encountered during sync down. Updating tier to FREE and aborting without DB modification.")
                subscriptionRepository.updateTier(com.victorkirui.core.repository.SubscriptionTier.FREE)
                Result.failure()
            } else if (code in 400..499) {
                // Client side error -> do not retry
                Result.failure()
            } else if (attempt < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        } catch (e: IOException) {
            Log.w(TAG, "Network IO error during sync-down: ${e.message}")
            if (attempt < MAX_RETRIES) Result.retry() else Result.failure()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during sync-down: ${e.message}", e)
            Result.failure()
        } finally {
            subscriptionRepository.setInitialSyncPending(false)
        }
    }

    companion object {
        private const val TAG = "SyncDownWorker"
        private const val SYNC_DOWN_WORK_NAME = "SyncDownHistory"
        private const val MAX_RETRIES = 2

        fun schedule(context: Context) {
            Log.d(TAG, "Scheduling unique sync-down work '$SYNC_DOWN_WORK_NAME'...")
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncDownWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                SYNC_DOWN_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
        }
    }
}
