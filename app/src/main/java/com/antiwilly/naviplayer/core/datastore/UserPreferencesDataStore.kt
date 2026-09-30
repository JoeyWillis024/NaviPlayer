package com.antiwilly.naviplayer.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val KEY_ACTIVE_SERVER_ID = longPreferencesKey("active_server_id")
        val KEY_EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val KEY_EQ_PRESET = stringPreferencesKey("eq_preset")
        val KEY_EQ_BANDS = stringPreferencesKey("eq_bands")
        val KEY_BASS_BOOST = intPreferencesKey("bass_boost")
        val KEY_SLEEP_TIMER_MIN = intPreferencesKey("sleep_timer_min")
        val KEY_AUTO_CACHE_ENABLED = booleanPreferencesKey("auto_cache_enabled")
    }

    val activeServerId: Flow<Long?> = dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_SERVER_ID]
    }

    val equalizerEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_EQ_ENABLED] ?: false
    }

    val equalizerPreset: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_EQ_PRESET] ?: "Flat"
    }

    val equalizerBands: Flow<List<Int>> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_EQ_BANDS] ?: "0,0,0,0,0"
        raw.split(",").mapNotNull { it.trim().toIntOrNull() }
    }

    val bassBoostStrength: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_BASS_BOOST] ?: 0
    }

    val sleepTimerDurationMin: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_SLEEP_TIMER_MIN] ?: 30
    }

    suspend fun setActiveServerId(id: Long) {
        dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_SERVER_ID] = id
        }
    }

    suspend fun setEqualizerEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_EQ_ENABLED] = enabled
        }
    }

    suspend fun setEqualizerPreset(preset: String) {
        dataStore.edit { prefs ->
            prefs[KEY_EQ_PRESET] = preset
        }
    }

    suspend fun setEqualizerBands(bands: List<Int>) {
        dataStore.edit { prefs ->
            prefs[KEY_EQ_BANDS] = bands.joinToString(",")
        }
    }

    suspend fun setBassBoostStrength(strength: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_BASS_BOOST] = strength
        }
    }

    suspend fun setSleepTimerDurationMin(minutes: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_SLEEP_TIMER_MIN] = minutes
        }
    }
}
