package com.kdlay.meaotodo.core.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")

class AppSettingsStore(private val context: Context) {
    val deviceRole: Flow<String> = context.appSettingsDataStore.data.map { preferences ->
        preferences[DEVICE_ROLE] ?: "main"
    }

    val pomodoroPreferences: Flow<PomodoroPreferences> = context.appSettingsDataStore.data.map { preferences ->
        PomodoroPreferences(
            focusDurationMinutes = (preferences[POMODORO_FOCUS_MINUTES] ?: PomodoroPreferences.DEFAULT_FOCUS_MINUTES)
                .coerceIn(1, 180),
            breakDurationMinutes = (preferences[POMODORO_BREAK_MINUTES] ?: PomodoroPreferences.DEFAULT_BREAK_MINUTES)
                .coerceIn(1, 120),
            targetFocusCount = (preferences[POMODORO_TARGET_FOCUS_COUNT] ?: PomodoroPreferences.DEFAULT_TARGET_FOCUS_COUNT)
                .coerceIn(1, 12),
            clockStyle = preferences[POMODORO_CLOCK_STYLE]?.takeIf { it in PomodoroPreferences.CLOCK_STYLES }
                ?: PomodoroPreferences.DEFAULT_CLOCK_STYLE
        )
    }

    suspend fun setDeviceRole(role: String) {
        context.appSettingsDataStore.edit { preferences ->
            preferences[DEVICE_ROLE] = role
        }
    }

    suspend fun setPomodoroFocusDurationMinutes(minutes: Int) {
        context.appSettingsDataStore.edit { preferences ->
            preferences[POMODORO_FOCUS_MINUTES] = minutes.coerceIn(1, 180)
        }
    }

    suspend fun setPomodoroBreakDurationMinutes(minutes: Int) {
        context.appSettingsDataStore.edit { preferences ->
            preferences[POMODORO_BREAK_MINUTES] = minutes.coerceIn(1, 120)
        }
    }

    suspend fun setPomodoroTargetFocusCount(count: Int) {
        context.appSettingsDataStore.edit { preferences ->
            preferences[POMODORO_TARGET_FOCUS_COUNT] = count.coerceIn(1, 12)
        }
    }

    suspend fun setPomodoroClockStyle(style: String) {
        context.appSettingsDataStore.edit { preferences ->
            preferences[POMODORO_CLOCK_STYLE] = style.takeIf { it in PomodoroPreferences.CLOCK_STYLES }
                ?: PomodoroPreferences.DEFAULT_CLOCK_STYLE
        }
    }

    private companion object {
        val DEVICE_ROLE = stringPreferencesKey("device_role")
        val POMODORO_FOCUS_MINUTES = intPreferencesKey("pomodoro_focus_minutes")
        val POMODORO_BREAK_MINUTES = intPreferencesKey("pomodoro_break_minutes")
        val POMODORO_TARGET_FOCUS_COUNT = intPreferencesKey("pomodoro_target_focus_count")
        val POMODORO_CLOCK_STYLE = stringPreferencesKey("pomodoro_clock_style")
    }
}

data class PomodoroPreferences(
    val focusDurationMinutes: Int = DEFAULT_FOCUS_MINUTES,
    val breakDurationMinutes: Int = DEFAULT_BREAK_MINUTES,
    val targetFocusCount: Int = DEFAULT_TARGET_FOCUS_COUNT,
    val clockStyle: String = DEFAULT_CLOCK_STYLE
) {
    companion object {
        const val DEFAULT_FOCUS_MINUTES = 25
        const val DEFAULT_BREAK_MINUTES = 5
        const val DEFAULT_TARGET_FOCUS_COUNT = 1
        const val DEFAULT_CLOCK_STYLE = "digital"
        const val CLOCK_STYLE_FLIP = "flip"
        val CLOCK_STYLES = setOf(DEFAULT_CLOCK_STYLE, CLOCK_STYLE_FLIP)
    }
}
