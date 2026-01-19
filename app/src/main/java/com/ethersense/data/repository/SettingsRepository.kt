package com.ethersense.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val AUDIO_ENABLED = booleanPreferencesKey("audio_enabled")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val LANGUAGE = stringPreferencesKey("language")
    }

    val audioEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.AUDIO_ENABLED] ?: false
        }

    val hapticEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.HAPTIC_ENABLED] ?: true
        }

    val language: Flow<AppLanguage> = context.dataStore.data
        .map { preferences ->
            val languageCode = preferences[PreferencesKeys.LANGUAGE] ?: AppLanguage.SYSTEM.code
            AppLanguage.fromCode(languageCode)
        }

    suspend fun setAudioEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUDIO_ENABLED] = enabled
        }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAPTIC_ENABLED] = enabled
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = language.code
        }
    }
}

enum class AppLanguage(val code: String, val displayName: String) {
    SYSTEM("system", "System Default / システム設定"),
    ENGLISH("en", "English"),
    JAPANESE("ja", "日本語");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.find { it.code == code } ?: SYSTEM
        }
    }
}
