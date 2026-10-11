package com.victorkirui.module_features.subscription

import android.content.Context
import android.util.Log
import androidx.work.*
import com.victorkirui.core.repository.AuthRepository
import com.victorkirui.core.repository.SubscriptionRepository
import com.victorkirui.core.repository.SubscriptionTier
import com.victorkirui.remote.CaptureApiService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class EntitlementRevalidationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val authRepository: AuthRepository by inject()
    private val subscriptionRepository: SubscriptionRepository by inject()
    private val captureApiService: CaptureApiService by inject()

    override suspend fun doWork(): Result {
        val user = authRepository.currentUser.value ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.d(TAG, "No user logged in. Skipping entitlement revalidation.")
            return Result.success()
        }

        try {
            Log.d(TAG, "Executing periodic entitlement revalidation with backend...")
            var isProUser = false

            // Query backend unified GET /v1/me endpoint (authoritative backend status)
            try {
                val profile = captureApiService.getMe()
                Log.d(TAG, "GET /v1/me revalidation: success=${profile.success}, isProUser=${profile.isProUser}, tier=${profile.tier}")
                if (profile.success) {
                    isProUser = profile.isProUser
                    if (profile.quotaLimit >= 0 && profile.quotaRemaining >= 0) {
                        subscriptionRepository.updateQuota(profile.quotaLimit, profile.quotaRemaining)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "GET /v1/me failed during background check: ${e.message}")
            }

            val currentTier = subscriptionRepository.subscriptionTier.first()
            val targetTier = if (isProUser) SubscriptionTier.PREMIUM else SubscriptionTier.FREE
            
            if (targetTier != currentTier) {
                Log.i(TAG, "Entitlement status changed via background revalidation: $currentTier -> $targetTier")
                subscriptionRepository.updateTier(targetTier)
            } else {
                Log.d(TAG, "Entitlement verified via background revalidation: Tier remains $targetTier")
            }
            
            if (targetTier != currentTier) {
                Log.i(TAG, "Entitlement status changed via background revalidation: $currentTier -> $targetTier")
                subscriptionRepository.updateTier(targetTier)
            } else {
                Log.d(TAG, "Entitlement verified via background revalidation: Tier remains $targetTier")
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Entitlement revalidation worker failed: ${e.message}", e)
            return Result.retry()
        }
    }


    companion object {
        private const val TAG = "EntitlementRevalidationWorker"
        private const val WORK_NAME = "PeriodicEntitlementRevalidationWork"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<EntitlementRevalidationWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
