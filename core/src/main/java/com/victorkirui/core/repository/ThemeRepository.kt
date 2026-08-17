package com.victorkirui.core.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "theme_settings")

interface ThemeRepository {
    val isDarkTheme: Flow<Boolean?>
    suspend fun setDarkTheme(isDark: Boolean?)
}

class ThemeRepositoryImpl(private val context: Context) : ThemeRepository {
    private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")

    override val isDarkTheme: Flow<Boolean?> = context.dataStore.data.map { preferences ->
        preferences[DARK_THEME_KEY]
    }

    override suspend fun setDarkTheme(isDark: Boolean?) {
        context.dataStore.edit { preferences ->
            if (isDark == null) {
                preferences.remove(DARK_THEME_KEY)
            } else {
                preferences[DARK_THEME_KEY] = isDark
            }
        }
    }
}
