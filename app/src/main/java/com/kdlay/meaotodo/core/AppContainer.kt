package com.kdlay.meaotodo.core

import android.content.Context
import androidx.room.Room
import com.kdlay.meaotodo.data.local.MeaoDatabase
import com.kdlay.meaotodo.data.repository.LedgerRepository
import com.kdlay.meaotodo.data.repository.PomodoroRepository
import com.kdlay.meaotodo.data.repository.TaskListRepository
import com.kdlay.meaotodo.data.repository.TaskRepository
import com.kdlay.meaotodo.core.settings.AppSettingsStore
import com.kdlay.meaotodo.ai.config.AiSettingsStore
import com.kdlay.meaotodo.ai.config.StoredAiProviderConfigSource
import com.kdlay.meaotodo.ai.config.StoredAiUsagePolicy
import com.kdlay.meaotodo.ai.network.OpenAiCompatibleClient
import com.kdlay.meaotodo.ai.AssistantAiService
import com.kdlay.meaotodo.domain.assistant.DailyContextRepository
import com.kdlay.meaotodo.domain.assistant.AssistantActionExecutor

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val settingsStore = AppSettingsStore(appContext)
    val aiSettingsStore = AiSettingsStore(appContext)
    val aiProviderConfigSource = StoredAiProviderConfigSource(aiSettingsStore)
    val aiUsagePolicy = StoredAiUsagePolicy(aiSettingsStore)
    val aiClient = OpenAiCompatibleClient()

    val database: MeaoDatabase = Room.databaseBuilder(
        appContext,
        MeaoDatabase::class.java,
        "meao_todo.db"
    ).addMigrations(*MeaoDatabase.ALL_MIGRATIONS).build()

    val taskRepository = TaskRepository(database.taskDao(), database.syncOutboxDao())
    val taskListRepository = TaskListRepository(database.taskListDao(), database.syncOutboxDao())
    val pomodoroRepository = PomodoroRepository(
        database.pomodoroDao(),
        database.pomodoroRunDao(),
        database.syncOutboxDao(),
        taskRepository
    )
    val ledgerRepository = LedgerRepository(database.ledgerDao(), database.syncOutboxDao())
    val dailyContextRepository = DailyContextRepository(
        taskRepository = taskRepository,
        pomodoroRepository = pomodoroRepository,
        ledgerRepository = ledgerRepository,
        settingsStore = settingsStore
    )
    val assistantAiService = AssistantAiService(
        providerConfigSource = aiProviderConfigSource,
        client = aiClient,
        dailyContextSource = dailyContextRepository,
        usagePolicy = aiUsagePolicy
    )
    val assistantActionExecutor = AssistantActionExecutor(
        taskRepository = taskRepository,
        pomodoroRepository = pomodoroRepository,
        ledgerRepository = ledgerRepository
    )
}
