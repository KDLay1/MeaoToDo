package com.kdlay.meaotodo.ui.plan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kdlay.meaotodo.ui.todo.TodoScreen
import com.kdlay.meaotodo.ui.todo.TodoViewModel

@Composable
fun PlanScreen(
    todoViewModel: TodoViewModel,
    onRequestTaskFocus: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TodoScreen(
        viewModel = todoViewModel,
        modifier = modifier,
        onStartFocus = { task -> onRequestTaskFocus(task.id) }
    )
}
