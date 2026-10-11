package com.victorkirui.module_features.di

import com.victorkirui.module_features.capturing.CapturingUseCase
import com.victorkirui.module_features.capturing.CapturingViewModel
import com.victorkirui.module_features.capturing.PdfTextExtractor
import com.victorkirui.module_features.details.ItemDetailsViewModel
import com.victorkirui.module_features.details.EditItemViewModel
import com.victorkirui.module_features.inbox.InboxViewModel
import com.victorkirui.module_features.reminder.ReminderScheduler
import com.victorkirui.module_features.reminders.RemindersViewModel
import com.victorkirui.module_features.details.CategoryDetailViewModel
import com.victorkirui.module_features.home.HomeViewModel
import com.victorkirui.module_features.profile.ProfileViewModel
import com.victorkirui.module_features.auth.SignUpViewModel
import com.victorkirui.module_features.auth.SignInViewModel
import com.victorkirui.module_features.capturing.ai.AiExtractor
import com.victorkirui.module_features.capturing.ai.RemindlyBackendExtractor
import com.victorkirui.module_features.onboarding.OnboardingViewModel
import com.victorkirui.module_features.BuildConfig
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

val featureModule = module {
    single { ReminderScheduler(androidContext(), get(), get()) }
    single { PdfTextExtractor(androidContext()) }
    
    single<AiExtractor> { 
        RemindlyBackendExtractor(
            context = androidContext(),
            subscriptionRepository = get(),
backendBaseUrl = "https://remindly-backend-v2.victorkirui-dev.workers.dev"
        ) 
    }
    factory { 
        CapturingUseCase(
            context = androidContext(),
            authRepository = get(),
            localRepository = get(),
            subscriptionRepository = get(),
            aiExtractor = get(),
            reminderScheduler = get(),
            calendarManager = get(),
            pdfTextExtractor = get(),
            notificationHelper = get(),
            timestampProvider = { 
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date())
            }
        ) 
    }
    viewModel { 
        CapturingViewModel(
            capturingUseCase = get(),
            notificationHelper = get(),
            analytics = get(),
            crashReporter = get()
        ) 
    }
    viewModel { ItemDetailsViewModel(get(), get(), get(), get(), get(), androidContext()) }
    viewModel { EditItemViewModel(get(), get(), get(), get(), get()) }
    viewModel { RemindersViewModel(get(), get()) }
    viewModel { InboxViewModel(get(), get(), get(), get()) }
    viewModel { HomeViewModel(get(), get(), get(), get(), androidContext()) }
    viewModel { CategoryDetailViewModel(get(), get(), get(), get()) }
    viewModel { 
        ProfileViewModel(
            settingsRepository = get(),
            subscriptionRepository = get(),
            authRepository = get(),
            reminderScheduler = get()
        ) 
    }
    viewModel { SignUpViewModel(get(), get(), get(), get()) }
    viewModel { SignInViewModel(get(), get(), get(), get()) }

    viewModel { OnboardingViewModel(get(), get()) }
    viewModel { com.victorkirui.module_features.nav.AppViewModel(get(), get()) }
}
