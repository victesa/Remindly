package com.victorkirui.core.di

import com.victorkirui.core.notification.NotificationHelper
import com.victorkirui.core.repository.ReminderSettingsRepository
import com.victorkirui.core.repository.ReminderSettingsRepositoryImpl
import com.victorkirui.core.repository.ThemeRepository
import com.victorkirui.core.repository.ThemeRepositoryImpl
import com.victorkirui.core.repository.AuthRepository
import com.victorkirui.core.repository.FirebaseAuthRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreModule = module {
    single { NotificationHelper(androidContext()) }
    single<ReminderSettingsRepository> { ReminderSettingsRepositoryImpl(androidContext()) }
    single<ThemeRepository> { ThemeRepositoryImpl(androidContext()) }
    single<AuthRepository> { FirebaseAuthRepository() }
}
