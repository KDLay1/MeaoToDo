package com.kdlay.meaotodo.ui.plan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kdlay.meaotodo.ui.components.MeaoSegmentedControl
import com.kdlay.meaotodo.ui.todo.PlanCalendarScreen
import com.kdlay.meaotodo.ui.todo.TodoScreen
import com.kdlay.meaotodo.ui.todo.TodoViewModel

@Composable
fun PlanScreen(
    todoViewModel: TodoViewModel,
    onRequestTaskFocus: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSection by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        MeaoSegmentedControl(
            options = listOf("任务", "日历"),
            selectedIndex = selectedSection,
            onSelect = { selectedSection = it },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
        if (selectedSection == 0) {
            TodoScreen(
                viewModel = todoViewModel,
                modifier = Modifier.weight(1f),
                onStartFocus = { task -> onRequestTaskFocus(task.id) }
            )
        } else {
            PlanCalendarScreen(
                viewModel = todoViewModel,
                modifier = Modifier.weight(1f),
                onStartFocus = { task -> onRequestTaskFocus(task.id) }
            )
        }
    }
}
