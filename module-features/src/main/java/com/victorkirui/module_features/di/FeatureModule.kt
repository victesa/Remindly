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
    factory { 
        CapturingUseCase(
            context = androidContext(),
            localRepository = get(),
            apiService = get(),
            reminderScheduler = get(),
            pdfTextExtractor = get(),
            timestampProvider = { 
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date())
            }
        ) 
    }
    viewModel { CapturingViewModel(get(), get()) }
    viewModel { ItemDetailsViewModel(get()) }
    viewModel { EditItemViewModel(get()) }
    viewModel { RemindersViewModel(get()) }
    viewModel { InboxViewModel(get()) }
    viewModel { HomeViewModel(get()) }
    viewModel { CategoryDetailViewModel(get()) }
    viewModel { 
        ProfileViewModel(
            settingsRepository = get(),
            themeRepository = get(),
            authRepository = get(),
            notificationHelper = get(),
            reminderScheduler = get(),
            localRepository = get()
        ) 
    }
    viewModel { SignUpViewModel(get()) }
    viewModel { SignInViewModel(get()) }
}
