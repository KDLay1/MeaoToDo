package com.kdlay.meaotodo.core.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")

class AppSettingsStore(private val context: Context) {
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

    val appPreferences: Flow<AppPreferences> = context.appSettingsDataStore.data.map { preferences ->
        AppPreferences(
            boardShowToday = preferences[BOARD_SHOW_TODAY] ?: true,
            boardShowPomodoro = preferences[BOARD_SHOW_POMODORO] ?: true,
            boardShowLedger = preferences[BOARD_SHOW_LEDGER] ?: true,
            boardShowSchedule = preferences[BOARD_SHOW_SCHEDULE] ?: true,
            boardShowStatus = preferences[BOARD_SHOW_STATUS] ?: true,
            monthlyBudgetCents = (preferences[MONTHLY_BUDGET_CENTS] ?: 0L).coerceAtLeast(0L)
        )
    }

    suspend fun setBoardShowToday(enabled: Boolean) = setBoolean(BOARD_SHOW_TODAY, enabled)
    suspend fun setBoardShowPomodoro(enabled: Boolean) = setBoolean(BOARD_SHOW_POMODORO, enabled)
    suspend fun setBoardShowLedger(enabled: Boolean) = setBoolean(BOARD_SHOW_LEDGER, enabled)
    suspend fun setBoardShowSchedule(enabled: Boolean) = setBoolean(BOARD_SHOW_SCHEDULE, enabled)
    suspend fun setBoardShowStatus(enabled: Boolean) = setBoolean(BOARD_SHOW_STATUS, enabled)
    suspend fun setMonthlyBudgetCents(cents: Long) {
        context.appSettingsDataStore.edit { preferences ->
            preferences[MONTHLY_BUDGET_CENTS] = cents.coerceAtLeast(0L)
        }
    }

    private suspend fun setBoolean(key: androidx.datastore.preferences.core.Preferences.Key<Boolean>, value: Boolean) {
        context.appSettingsDataStore.edit { preferences -> preferences[key] = value }
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
        val POMODORO_FOCUS_MINUTES = intPreferencesKey("pomodoro_focus_minutes")
        val POMODORO_BREAK_MINUTES = intPreferencesKey("pomodoro_break_minutes")
        val POMODORO_TARGET_FOCUS_COUNT = intPreferencesKey("pomodoro_target_focus_count")
        val POMODORO_CLOCK_STYLE = stringPreferencesKey("pomodoro_clock_style")
        val BOARD_SHOW_TODAY = booleanPreferencesKey("board_show_today")
        val BOARD_SHOW_POMODORO = booleanPreferencesKey("board_show_pomodoro")
        val BOARD_SHOW_LEDGER = booleanPreferencesKey("board_show_ledger")
        val BOARD_SHOW_SCHEDULE = booleanPreferencesKey("board_show_schedule")
        val BOARD_SHOW_STATUS = booleanPreferencesKey("board_show_status")
        val MONTHLY_BUDGET_CENTS = longPreferencesKey("monthly_budget_cents")
    }
}

data class AppPreferences(
    val boardShowToday: Boolean = true,
    val boardShowPomodoro: Boolean = true,
    val boardShowLedger: Boolean = true,
    val boardShowSchedule: Boolean = true,
    val boardShowStatus: Boolean = true,
    val monthlyBudgetCents: Long = 0L
)

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
