package com.kdlay.meaotodo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.kdlay.meaotodo.core.AppContainer
import com.kdlay.meaotodo.ui.MeaoTodoApp
import com.kdlay.meaotodo.ui.todo.TodoViewModel
import com.kdlay.meaotodo.ui.theme.MeaoTodoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appContainer = AppContainer(applicationContext)
        val todoViewModel = ViewModelProvider(
            this,
            TodoViewModel.factory(appContainer.taskRepository)
        )[TodoViewModel::class.java]

        setContent {
            MeaoTodoTheme {
                MeaoTodoApp(todoViewModel = todoViewModel)
            }
        }
    }
}
