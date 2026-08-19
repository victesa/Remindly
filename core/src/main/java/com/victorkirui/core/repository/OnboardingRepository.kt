package com.victorkirui.core.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding_prefs")

interface OnboardingRepository {
    val hasSeenOnboarding: Flow<Boolean>
    suspend fun setHasSeenOnboarding(hasSeen: Boolean)
}

class OnboardingRepositoryImpl(private val context: Context) : OnboardingRepository {
    private val HAS_SEEN_ONBOARDING_KEY = booleanPreferencesKey("has_seen_onboarding")

    override val hasSeenOnboarding: Flow<Boolean> = context.onboardingDataStore.data.map { preferences ->
        preferences[HAS_SEEN_ONBOARDING_KEY] ?: false
    }

    override suspend fun setHasSeenOnboarding(hasSeen: Boolean) {
        context.onboardingDataStore.edit { preferences ->
            preferences[HAS_SEEN_ONBOARDING_KEY] = hasSeen
        }
    }
}
