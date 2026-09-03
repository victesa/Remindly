package com.victorkirui.core.di

import com.victorkirui.core.notification.NotificationHelper
import com.victorkirui.core.repository.ReminderSettingsRepository
import com.victorkirui.core.repository.ReminderSettingsRepositoryImpl
import com.victorkirui.core.repository.AuthRepository
import com.victorkirui.core.repository.FirebaseAuthRepository
import com.victorkirui.core.repository.OnboardingRepository
import com.victorkirui.core.repository.OnboardingRepositoryImpl
import com.victorkirui.core.monitoring.Analytics
import com.victorkirui.core.monitoring.FirebaseAnalyticsImpl
import com.victorkirui.core.monitoring.CrashReporter
import com.victorkirui.core.monitoring.FirebaseCrashReporterImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreModule = module {
    single { NotificationHelper(androidContext()) }
    single<ReminderSettingsRepository> { ReminderSettingsRepositoryImpl(androidContext()) }
    single<com.victorkirui.core.data.DataClearer> { get<ReminderSettingsRepository>() }
    single<com.victorkirui.core.repository.SubscriptionRepository> { com.victorkirui.core.repository.SubscriptionRepositoryImpl(androidContext()) }
    single<com.victorkirui.core.data.DataClearer> { get<com.victorkirui.core.repository.SubscriptionRepository>() }
    single { com.victorkirui.core.billing.BillingManager(androidContext(), get()) }
    single<AuthRepository> { FirebaseAuthRepository(androidContext(), get(), get(), getAll()) }
    single<OnboardingRepository> { OnboardingRepositoryImpl(androidContext()) }
    single<Analytics> { FirebaseAnalyticsImpl() }
    single<CrashReporter> { FirebaseCrashReporterImpl() }
    single { com.victorkirui.core.calendar.CalendarManager(androidContext()) }
}
