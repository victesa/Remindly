package com.victorkirui.core.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "reminder_settings")

interface ReminderSettingsRepository : com.victorkirui.core.data.DataClearer {
    val preferredReminderTime: Flow<String>
    val isMorningBriefingEnabled: Flow<Boolean>
    val isDeadlineAlertsEnabled: Flow<Boolean>
    val deadlineAlertLeadTimeHours: Flow<Int>
    
    suspend fun setPreferredReminderTime(time: String)
    suspend fun setMorningBriefingEnabled(enabled: Boolean)
    suspend fun setDeadlineAlertsEnabled(enabled: Boolean)
    suspend fun setDeadlineAlertLeadTimeHours(hours: Int)
}

class ReminderSettingsRepositoryImpl(private val context: Context) : ReminderSettingsRepository {
    private val PREFERRED_TIME_KEY = stringPreferencesKey("preferred_reminder_time")
    private val MORNING_BRIEFING_KEY = booleanPreferencesKey("morning_briefing_enabled")
    private val DEADLINE_ALERTS_KEY = booleanPreferencesKey("deadline_alerts_enabled")
    private val DEADLINE_LEAD_TIME_KEY = androidx.datastore.preferences.core.intPreferencesKey("deadline_alert_lead_time")

    override val preferredReminderTime: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PREFERRED_TIME_KEY] ?: "08:00"
    }

    override val isMorningBriefingEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[MORNING_BRIEFING_KEY] ?: true
    }

    override val isDeadlineAlertsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DEADLINE_ALERTS_KEY] ?: true
    }

    override val deadlineAlertLeadTimeHours: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DEADLINE_LEAD_TIME_KEY] ?: 1
    }

    override suspend fun setPreferredReminderTime(time: String) {
        context.dataStore.edit { preferences ->
            preferences[PREFERRED_TIME_KEY] = time
        }
    }

    override suspend fun setMorningBriefingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MORNING_BRIEFING_KEY] = enabled
        }
    }

    override suspend fun setDeadlineAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DEADLINE_ALERTS_KEY] = enabled
        }
    }

    override suspend fun setDeadlineAlertLeadTimeHours(hours: Int) {
        context.dataStore.edit { preferences ->
            preferences[DEADLINE_LEAD_TIME_KEY] = hours
        }
    }

    override suspend fun clearUserData(userId: String) {
        context.dataStore.edit { it.clear() }
    }
}
