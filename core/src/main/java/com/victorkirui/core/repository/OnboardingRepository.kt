package com.victorkirui.core.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding_prefs")

interface OnboardingRepository : com.victorkirui.core.data.DataClearer {
    val hasSeenOnboarding: Flow<Boolean>
    val hasSeenHomeWalkthrough: Flow<Boolean>
    suspend fun setHasSeenOnboarding(hasSeen: Boolean)
    suspend fun setHasSeenHomeWalkthrough(hasSeen: Boolean)
    suspend fun resetOnboarding()
}

class OnboardingRepositoryImpl(private val context: Context) : OnboardingRepository {
    private val HAS_SEEN_ONBOARDING_KEY = booleanPreferencesKey("has_seen_onboarding")
    private val HAS_SEEN_WALKTHROUGH_KEY = booleanPreferencesKey("has_seen_home_walkthrough")

    override val hasSeenOnboarding: Flow<Boolean> = context.onboardingDataStore.data
        .catch { e ->
            if (e is IOException) {
                emit(emptyPreferences())
            } else {
                throw e
            }
        }
        .map { preferences ->
            preferences[HAS_SEEN_ONBOARDING_KEY] ?: false
        }

    override val hasSeenHomeWalkthrough: Flow<Boolean> = context.onboardingDataStore.data
        .catch { e ->
            if (e is IOException) {
                emit(emptyPreferences())
            } else {
                throw e
            }
        }
        .map { preferences ->
            preferences[HAS_SEEN_WALKTHROUGH_KEY] ?: true
        }

    override suspend fun setHasSeenOnboarding(hasSeen: Boolean) {
        context.onboardingDataStore.edit { preferences ->
            preferences[HAS_SEEN_ONBOARDING_KEY] = hasSeen
        }
    }

    override suspend fun setHasSeenHomeWalkthrough(hasSeen: Boolean) {
        context.onboardingDataStore.edit { preferences ->
            preferences[HAS_SEEN_WALKTHROUGH_KEY] = hasSeen
        }
    }

    override suspend fun resetOnboarding() {
        context.onboardingDataStore.edit { preferences ->
            preferences[HAS_SEEN_ONBOARDING_KEY] = false
            preferences.remove(HAS_SEEN_WALKTHROUGH_KEY)
        }
    }

    override suspend fun clearUserData(userId: String) {
        context.onboardingDataStore.edit { preferences ->
            preferences.remove(HAS_SEEN_WALKTHROUGH_KEY)
        }
    }
}
