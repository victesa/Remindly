package com.victorkirui.remindly

import android.app.Application
import com.victorkirui.core.di.coreModule
import com.victorkirui.local.di.localModule
import com.victorkirui.module_features.di.featureModule
import com.victorkirui.remote.di.remoteModule
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
    }
}
