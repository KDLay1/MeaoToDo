package com.kdlay.meaotodo

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.kdlay.meaotodo.core.AppContainer
import com.kdlay.meaotodo.ui.MeaoTodoApp
import com.kdlay.meaotodo.ui.board.BoardViewModel
import com.kdlay.meaotodo.ui.ledger.LedgerViewModel
import com.kdlay.meaotodo.ui.settings.SettingsViewModel
import com.kdlay.meaotodo.ui.theme.MeaoTodoTheme
import com.kdlay.meaotodo.ui.timer.PomodoroViewModel
import com.kdlay.meaotodo.ui.todo.TodoViewModel
import com.kdlay.meaotodo.ui.assistant.AssistantViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appContainer = AppContainer(applicationContext)
        val todoViewModel = ViewModelProvider(
            this,
            TodoViewModel.factory(appContainer.taskRepository, appContainer.taskListRepository)
        )[TodoViewModel::class.java]
        val pomodoroViewModel = ViewModelProvider(
            this,
            PomodoroViewModel.factory(appContainer.pomodoroRepository, appContainer.settingsStore, appContainer.taskRepository)
        )[PomodoroViewModel::class.java]
        val ledgerViewModel = ViewModelProvider(
            this,
            LedgerViewModel.factory(appContainer.ledgerRepository)
        )[LedgerViewModel::class.java]
        val boardViewModel = ViewModelProvider(
            this,
            BoardViewModel.factory(appContainer.taskRepository, appContainer.pomodoroRepository, appContainer.ledgerRepository)
        )[BoardViewModel::class.java]
        val settingsViewModel = ViewModelProvider(
            this,
            SettingsViewModel.factory(
                appContainer.settingsStore,
                appContainer.aiSettingsStore,
                appContainer.assistantAiService
            )
        )[SettingsViewModel::class.java]
        val assistantViewModel = ViewModelProvider(
            this,
            AssistantViewModel.factory(
                appContainer.dailyContextRepository,
                appContainer.aiSettingsStore,
                appContainer.assistantAiService,
                appContainer.assistantActionExecutor
            )
        )[AssistantViewModel::class.java]

        setContent {
            MeaoTodoTheme {
                MeaoTodoApp(
                    todoViewModel = todoViewModel,
                    pomodoroViewModel = pomodoroViewModel,
                    ledgerViewModel = ledgerViewModel,
                    boardViewModel = boardViewModel,
                    settingsViewModel = settingsViewModel,
                    assistantViewModel = assistantViewModel,
                    onTimerImmersiveModeChange = { isImmersive ->
                        requestedOrientation = if (isImmersive) {
                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        } else {
                            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        }
                    }
                )
            }
        }
    }
}
