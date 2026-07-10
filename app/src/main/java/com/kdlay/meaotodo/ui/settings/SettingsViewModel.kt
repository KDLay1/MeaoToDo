package com.kdlay.meaotodo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kdlay.meaotodo.core.settings.AppSettingsStore
import com.kdlay.meaotodo.core.settings.AppPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsStore: AppSettingsStore
) : ViewModel() {
    val preferences: StateFlow<AppPreferences> = settingsStore.appPreferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppPreferences()
    )

    fun setBoardShowToday(enabled: Boolean) = viewModelScope.launch { settingsStore.setBoardShowToday(enabled) }
    fun setBoardShowPomodoro(enabled: Boolean) = viewModelScope.launch { settingsStore.setBoardShowPomodoro(enabled) }
    fun setBoardShowLedger(enabled: Boolean) = viewModelScope.launch { settingsStore.setBoardShowLedger(enabled) }
    fun setBoardShowSchedule(enabled: Boolean) = viewModelScope.launch { settingsStore.setBoardShowSchedule(enabled) }
    fun setBoardShowStatus(enabled: Boolean) = viewModelScope.launch { settingsStore.setBoardShowStatus(enabled) }
    fun setMonthlyBudgetCents(cents: Long) = viewModelScope.launch { settingsStore.setMonthlyBudgetCents(cents) }

    companion object {
        fun factory(settingsStore: AppSettingsStore): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(SettingsViewModel::class.java))
                return SettingsViewModel(settingsStore) as T
            }
        }
    }
}
