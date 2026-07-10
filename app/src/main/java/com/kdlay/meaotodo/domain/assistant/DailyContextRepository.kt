package com.kdlay.meaotodo.domain.assistant

import com.kdlay.meaotodo.core.settings.AppSettingsStore
import com.kdlay.meaotodo.data.repository.LedgerRepository
import com.kdlay.meaotodo.data.repository.PomodoroRepository
import com.kdlay.meaotodo.data.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

interface DailyContextSource {
    val context: Flow<DailyContext>
}

class DailyContextRepository(
    taskRepository: TaskRepository,
    pomodoroRepository: PomodoroRepository,
    ledgerRepository: LedgerRepository,
    settingsStore: AppSettingsStore,
    private val nowProvider: () -> Long = System::currentTimeMillis,
    private val builder: DailyContextBuilder = DailyContextBuilder()
) : DailyContextSource {
    override val context: Flow<DailyContext> = combine(
        taskRepository.activeTasks,
        pomodoroRepository.sessions,
        ledgerRepository.entries,
        pomodoroRepository.activeSession,
        settingsStore.appPreferences
    ) { tasks, sessions, entries, activeSession, preferences ->
        builder.build(
            tasks = tasks,
            sessions = sessions,
            ledgerEntries = entries,
            activeSession = activeSession,
            monthlyBudgetCents = preferences.monthlyBudgetCents,
            now = nowProvider()
        )
    }
}
