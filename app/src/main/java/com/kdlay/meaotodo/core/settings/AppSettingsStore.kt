package com.kdlay.meaotodo.core.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")

class AppSettingsStore(private val context: Context) {
    val deviceRole: Flow<String> = context.appSettingsDataStore.data.map { preferences ->
        preferences[DEVICE_ROLE] ?: "main"
    }

    suspend fun setDeviceRole(role: String) {
        context.appSettingsDataStore.edit { preferences ->
            preferences[DEVICE_ROLE] = role
        }
    }

    private companion object {
        val DEVICE_ROLE = stringPreferencesKey("device_role")
    }
}
