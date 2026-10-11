package com.victorkirui.remindly

import android.app.Application
import com.victorkirui.core.di.coreModule
import com.victorkirui.local.di.localModule
import com.victorkirui.remote.di.remoteModule
import com.victorkirui.module_features.di.featureModule
import com.victorkirui.core.monitoring.Analytics
import com.victorkirui.core.repository.SubscriptionRepository
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class RemindlyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@RemindlyApp)
            modules(coreModule, localModule, remoteModule, featureModule)
        }

        val analytics: Analytics = get()
        val subscriptionRepository: SubscriptionRepository = get()
        val localRepository: com.victorkirui.local.repository.LocalRepository = get()
        val authRepository: com.victorkirui.core.repository.AuthRepository = get()

        subscriptionRepository.onSyncLocalItemsToBackend = {
            val userId = authRepository.currentUser.value?.uid
            if (userId != null) {
                MainScope().launch {
                    val items = localRepository.getAllItems(userId).first()
                    var reprocessedCount = 0
                    items.forEach { item ->
                        if (item.status == "FAILED" || item.status == "RETRYING" || item.status == "PENDING") {
                            val resetItem = item.copy(status = "PENDING", title = "Analysing...")
                            localRepository.saveItem(resetItem)
                            localRepository.addPendingSync(com.victorkirui.local.entity.PendingSync(itemId = item.id))
                            reprocessedCount++
                        }
                    }
                    if (reprocessedCount > 0) {
                        com.victorkirui.module_features.capturing.SyncWorker.schedule(this@RemindlyApp)
                        android.util.Log.d("RemindlyApp", "Enqueued $reprocessedCount quota-failed/pending items for AI re-analysis after Pro upgrade.")
                    }
                }
            }
        }

        analytics.logEvent("app_start")

        // Log Firebase Installation ID (FID) for testing Firebase In-App Messaging campaigns
        com.google.firebase.installations.FirebaseInstallations.getInstance().id.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                android.util.Log.i("FIAM_TEST", "Firebase Installation ID (FID) for testing: ${task.result}")
            }
        }

        // Log FCM Registration Token for testing push notifications
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                android.util.Log.i("FCM_TOKEN", "FCM Registration Token for testing: ${task.result}")
            }
        }


        // Schedule periodic background entitlement revalidation with backend (every 12 hours)
        com.victorkirui.module_features.subscription.EntitlementRevalidationWorker.schedule(this)


        MainScope().launch {
            subscriptionRepository.subscriptionTier.distinctUntilChanged().collect { tier ->
                android.util.Log.d("RemindlyApp", "Subscription tier flow emitted: $tier. Refreshing Glance widgets...")
                com.victorkirui.remindly.widget.WidgetUpdater.updateWidgets(this@RemindlyApp)
            }
        }
    }
}
