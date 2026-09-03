package com.victorkirui.module_features.capturing

import android.content.Context
import android.util.Log
import androidx.work.*
import com.victorkirui.core.repository.AuthRepository
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

        try {
            val quotaResponse = captureApiService.getQuota()
            val quota = quotaResponse.quota
            if (quotaResponse.success && quota != null) {
                Log.d(TAG, "Quota refreshed: Tier=${quota.tier}, Limit=${quota.limit}, Remaining=${quota.remaining}")
                subscriptionRepository.updateQuota(quota.limit, quota.remaining)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Quota check skipped: ${e.message}")
        }

        val tier = subscriptionRepository.subscriptionTier.first()
        if (tier != com.victorkirui.core.repository.SubscriptionTier.PREMIUM) {
            Log.d(TAG, "User tier is $tier. Skipping sync-down (Premium feature).")
            return Result.success()
        }

        return try {
            val remoteItems = captureApiService.getCaptures()
            Log.d(TAG, "Successfully fetched ${remoteItems.size} cloud items for user $userId.")
            
            localRepository.withTransaction {
                remoteItems.forEach { remote ->
                    val payload = remote.data ?: return@forEach

                    // Check if a local item (e.g., 'local-uuid...') exists for this same content and remove it to avoid duplication
                    val existingDuplicate = localRepository.findDuplicateEx(
                        userId = userId,
                        url = payload.url,
                        text = remote.inputSnippet,
                        title = payload.title
                    ) ?: localRepository.getItem(remote.id)

                    if (existingDuplicate != null && existingDuplicate.id != remote.id) {
                        Log.d(TAG, "Deduplicating: Replacing temporary local item ${existingDuplicate.id} with cloud item ${remote.id}")
                        localRepository.deleteItem(userId, existingDuplicate.id)
                    }

                    val localItem = Item(
                        id = remote.id,
                        userId = userId,
                        title = payload.title,
                        summary = payload.summary,
                        category = payload.category,
                        deadline = payload.deadline,
                        eventDate = payload.eventDate,
                        organization = payload.organization,
                        source = null,
                        sourceUrl = payload.url,
                        originalMediaUri = null,
                        metadata = mapOf(
                            "strategy" to payload.strategy,
                            "tier" to payload.tier,
                            "source_type" to (remote.sourceType ?: "OTHER"),
                            "extracted_at" to (remote.extractedAt ?: "")
                        ),
                        extractedText = remote.inputSnippet,
                        contentType = when(remote.sourceType) {
                            "image" -> "IMAGE"
                            "text" -> "TEXT"
                            "url" -> "TEXT"
                            else -> "OTHER"
                        },
                        checklist = payload.actionableItems,
                        status = "READY",
                        createdAt = remote.extractedAt ?: ""
                    )
                    localRepository.saveItem(localItem)
                    reminderScheduler.scheduleRemindersForItem(localItem)
                }
            }
            Log.d(TAG, "Sync-down complete. ${remoteItems.size} items stored locally.")
            Result.success()
        } catch (e: HttpException) {
            val code = e.code()
            val errorBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
            Log.e(TAG, "HTTP $code error during sync-down. Response: $errorBody", e)
            if (code in 400..499) {
                // Client side error (e.g. 403 Forbidden, 401 Unauthorized) -> do not retry
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
