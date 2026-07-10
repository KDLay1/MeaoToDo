package com.kdlay.meaotodo.ui.plan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kdlay.meaotodo.ui.timer.PomodoroTemplateScreen
import com.kdlay.meaotodo.ui.timer.PomodoroViewModel
import com.kdlay.meaotodo.ui.todo.TodoScreen
import com.kdlay.meaotodo.ui.todo.TodoViewModel

enum class PlanSection { TASKS, FOCUS }

@Composable
fun PlanScreen(
    section: PlanSection,
    onSectionChange: (PlanSection) -> Unit,
    todoViewModel: TodoViewModel,
    pomodoroViewModel: PomodoroViewModel,
    requestedPomodoroTaskId: String?,
    onRequestedStartTaskHandled: () -> Unit,
    onRequestTaskFocus: (String) -> Unit,
    onImmersiveModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
            FilterChip(
                selected = section == PlanSection.TASKS,
                onClick = { onSectionChange(PlanSection.TASKS) },
                label = { Text("任务与日历") }
            )
            FilterChip(
                modifier = Modifier.padding(start = 8.dp),
                selected = section == PlanSection.FOCUS,
                onClick = { onSectionChange(PlanSection.FOCUS) },
                label = { Text("专注") }
            )
        }
        when (section) {
            PlanSection.TASKS -> TodoScreen(
                viewModel = todoViewModel,
                onStartFocus = { task -> onRequestTaskFocus(task.id) }
            )
            PlanSection.FOCUS -> PomodoroTemplateScreen(
                viewModel = pomodoroViewModel,
                requestedStartTaskId = requestedPomodoroTaskId,
                onRequestedStartTaskHandled = onRequestedStartTaskHandled,
                onImmersiveModeChange = onImmersiveModeChange
            )
        }
    }
}
