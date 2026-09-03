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

        analytics.logEvent("app_start")

        MainScope().launch {
            subscriptionRepository.subscriptionTier.distinctUntilChanged().collect { tier ->
                android.util.Log.d("RemindlyApp", "Subscription tier flow emitted: $tier. Refreshing Glance widgets...")
                com.victorkirui.remindly.widget.WidgetUpdater.updateWidgets(this@RemindlyApp)
            }
        }
    }
}
