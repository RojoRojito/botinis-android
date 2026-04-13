package io.botinis.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    private val context: Context
) {
    private val dataStore = context.settingsDataStore

    val groqApiKey: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_GROQ_API_KEY] ?: ""
    }

    suspend fun saveGroqApiKey(apiKey: String) {
        dataStore.edit { preferences ->
            preferences[KEY_GROQ_API_KEY] = apiKey
        }
    }

    companion object {
        private val KEY_GROQ_API_KEY = stringPreferencesKey("groq_api_key")
    }
}
