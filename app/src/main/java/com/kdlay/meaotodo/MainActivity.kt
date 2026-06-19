package com.kdlay.meaotodo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.kdlay.meaotodo.core.AppContainer
import com.kdlay.meaotodo.ui.MeaoTodoApp
import com.kdlay.meaotodo.ui.theme.MeaoTodoTheme
import com.kdlay.meaotodo.ui.timer.PomodoroViewModel
import com.kdlay.meaotodo.ui.todo.TodoViewModel

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
            PomodoroViewModel.factory(appContainer.pomodoroRepository, appContainer.taskRepository)
        )[PomodoroViewModel::class.java]

        setContent {
            MeaoTodoTheme {
                MeaoTodoApp(
                    todoViewModel = todoViewModel,
                    pomodoroViewModel = pomodoroViewModel
                )
            }
        }
    }
}
