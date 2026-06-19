package com.kdlay.meaotodo.ui.todo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kdlay.meaotodo.data.local.entity.TaskEntity

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DisplayModeSwitcher(
    displayMode: TodoDisplayMode,
    calendarMode: TodoCalendarMode,
    onDisplayModeChange: (TodoDisplayMode) -> Unit,
    onCalendarModeChange: (TodoCalendarMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TodoDisplayMode.entries.forEach { mode ->
                if (displayMode == mode) {
                    FilledTonalButton(onClick = { onDisplayModeChange(mode) }) { Text(mode.label) }
                } else {
                    OutlinedButton(onClick = { onDisplayModeChange(mode) }) { Text(mode.label) }
                }
            }
        }
        if (displayMode == TodoDisplayMode.Calendar) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TodoCalendarMode.entries.forEach { mode ->
                    if (calendarMode == mode) {
                        FilledTonalButton(onClick = { onCalendarModeChange(mode) }) { Text(mode.label) }
                    } else {
                        OutlinedButton(onClick = { onCalendarModeChange(mode) }) { Text(mode.label) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TodoCalendarContent(
    groups: TodoGroups,
    selectedList: TodoListOption,
    calendarMode: TodoCalendarMode,
    selectedDate: Long,
    onSelectedDateChange: (Long) -> Unit,
    onCalendarModeChange: (TodoCalendarMode) -> Unit,
    onCheckedChange: (TaskEntity, Boolean) -> Unit,
    onEdit: (TaskEntity) -> Unit,
    onRemove: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val calendarTasks = groups.tasksFor(selectedList.id).filter { it.dueAt != null }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (calendarMode) {
            TodoCalendarMode.Week -> WeekCalendarView(
                tasks = calendarTasks,
                selectedDate = selectedDate,
                onSelectedDateChange = onSelectedDateChange,
                onEdit = onEdit
            )
            TodoCalendarMode.Month -> MonthCalendarView(
                tasks = calendarTasks,
                selectedDate = selectedDate,
                onSelectedDateChange = onSelectedDateChange,
                onCheckedChange = onCheckedChange,
                onEdit = onEdit,
                onRemove = onRemove
            )
            TodoCalendarMode.Year -> YearCalendarView(
                tasks = calendarTasks,
                selectedDate = selectedDate,
                onSelectedDateChange = onSelectedDateChange,
                onCalendarModeChange = onCalendarModeChange
            )
        }
    }
}

@Composable
private fun WeekCalendarView(
    tasks: List<TaskEntity>,
    selectedDate: Long,
    onSelectedDateChange: (Long) -> Unit,
    onEdit: (TaskEntity) -> Unit
) {
    val weekStart = startOfWeek(selectedDate)
    val weekDays = (0..6).map { addDays(weekStart, it) }
    val scrollState = rememberScrollState()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CalendarNavigationHeader(
            title = "${formatDate(weekStart)} - ${formatDate(addDays(weekStart, 6))}",
            onPrevious = { onSelectedDateChange(addDays(weekStart, -7)) },
            onToday = { onSelectedDateChange(System.currentTimeMillis()) },
            onNext = { onSelectedDateChange(addDays(weekStart, 7)) }
        )

        Column(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("时间", modifier = Modifier.width(54.dp), style = MaterialTheme.typography.labelMedium)
                weekDays.forEach { day ->
                    DayHeaderCell(day = day, selectedDate = selectedDate, onClick = { onSelectedDateChange(day) })
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("全天", modifier = Modifier.width(54.dp).padding(top = 8.dp), style = MaterialTheme.typography.labelSmall)
                weekDays.forEach { day ->
                    val dayTasks = tasksForDay(tasks, day).filterNot { it.hasDueTime }
                    WeekTaskCell(tasks = dayTasks, onEdit = onEdit)
                }
            }

            (0..23).forEach { hour ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "%02d:00".format(hour),
                        modifier = Modifier.width(54.dp).padding(top = 8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    weekDays.forEach { day ->
                        val hourTasks = tasksForDay(tasks, day)
                            .filter { it.hasDueTime && hourOf(it.dueAt!!) == hour }
                            .sortedBy { it.dueAt }
                        WeekTaskCell(tasks = hourTasks, onEdit = onEdit)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeaderCell(day: Long, selectedDate: Long, onClick: () -> Unit) {
    val selected = startOfDay(day) == startOfDay(selectedDate)
    val today = isToday(day)
    val label = "${weekdayLabel(day)}\n${dayOfMonth(day)}"
    if (selected || today) {
        FilledTonalButton(onClick = onClick, modifier = Modifier.width(96.dp)) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.width(96.dp)) { Text(label) }
    }
}

@Composable
private fun WeekTaskCell(tasks: List<TaskEntity>, onEdit: (TaskEntity) -> Unit) {
    Surface(
        modifier = Modifier.width(96.dp).heightIn(min = 58.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (tasks.isEmpty()) {
                Text(" ", style = MaterialTheme.typography.labelSmall)
            } else {
                tasks.take(3).forEach { task -> MiniCalendarTask(task = task, onClick = { onEdit(task) }) }
                if (tasks.size > 3) {
                    Text("+${tasks.size - 3}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun MiniCalendarTask(task: TaskEntity, onClick: () -> Unit) {
    val prefix = if (task.hasDueTime && task.dueAt != null) "${formatTime(task.dueAt)} " else ""
    Text(
        text = "$prefix${task.title}",
        modifier = Modifier.clickable(onClick = onClick),
        style = MaterialTheme.typography.labelSmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textDecoration = if (task.isDone) TextDecoration.LineThrough else null
    )
}

@Composable
private fun MonthCalendarView(
    tasks: List<TaskEntity>,
    selectedDate: Long,
    onSelectedDateChange: (Long) -> Unit,
    onCheckedChange: (TaskEntity, Boolean) -> Unit,
    onEdit: (TaskEntity) -> Unit,
    onRemove: (TaskEntity) -> Unit
) {
    val monthStart = startOfMonth(selectedDate)
    val gridStart = startOfWeek(monthStart)
    val selectedTasks = tasksForDay(tasks, selectedDate)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CalendarNavigationHeader(
            title = monthTitle(monthStart),
            onPrevious = { onSelectedDateChange(addMonths(monthStart, -1)) },
            onToday = { onSelectedDateChange(System.currentTimeMillis()) },
            onNext = { onSelectedDateChange(addMonths(monthStart, 1)) }
        )

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                    Text(it, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                }
            }
            (0..5).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (0..6).forEach { offset ->
                        val day = addDays(gridStart, week * 7 + offset)
                        MonthDayCell(
                            day = day,
                            monthStart = monthStart,
                            selectedDate = selectedDate,
                            tasks = tasksForDay(tasks, day),
                            onClick = { onSelectedDateChange(day) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Text("${formatDate(selectedDate)} · ${selectedTasks.size} 项", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        if (selectedTasks.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Text("这一天没有设置截止日期的任务。", modifier = Modifier.padding(18.dp))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 260.dp)) {
                items(selectedTasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        onCheckedChange = { onCheckedChange(task, it) },
                        onEdit = { onEdit(task) },
                        onRemove = { onRemove(task) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthDayCell(
    day: Long,
    monthStart: Long,
    selectedDate: Long,
    tasks: List<TaskEntity>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = startOfDay(day) == startOfDay(selectedDate)
    val inMonth = monthOf(day) == monthOf(monthStart)
    Surface(
        modifier = modifier.heightIn(min = 76.dp).clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = dayOfMonth(day).toString(),
                style = MaterialTheme.typography.labelMedium,
                color = if (inMonth) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (tasks.isNotEmpty()) {
                Text("${tasks.size}项", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                tasks.take(2).forEach { task -> MiniCalendarTask(task = task, onClick = onClick) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun YearCalendarView(
    tasks: List<TaskEntity>,
    selectedDate: Long,
    onSelectedDateChange: (Long) -> Unit,
    onCalendarModeChange: (TodoCalendarMode) -> Unit
) {
    val year = yearOf(selectedDate)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CalendarNavigationHeader(
            title = "${year} 年",
            onPrevious = { onSelectedDateChange(startOfYear(year - 1)) },
            onToday = { onSelectedDateChange(System.currentTimeMillis()) },
            onNext = { onSelectedDateChange(startOfYear(year + 1)) }
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (0..11).forEach { month ->
                val monthStart = startOfMonth(year, month)
                val monthEnd = addMonths(monthStart, 1)
                val monthTasks = tasks.filter { task ->
                    val dueAt = task.dueAt
                    dueAt != null && dueAt >= monthStart && dueAt < monthEnd
                }
                Surface(
                    modifier = Modifier.width(104.dp).clickable {
                        onSelectedDateChange(monthStart)
                        onCalendarModeChange(TodoCalendarMode.Month)
                    },
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${month + 1}月", fontWeight = FontWeight.SemiBold)
                        Text("${monthTasks.size} 项", style = MaterialTheme.typography.bodySmall)
                        Text("未完成 ${monthTasks.count { !it.isDone }}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarNavigationHeader(
    title: String,
    onPrevious: () -> Unit,
    onToday: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = onPrevious) { Text("‹") }
            OutlinedButton(onClick = onToday) { Text("今天") }
            OutlinedButton(onClick = onNext) { Text("›") }
        }
    }
}
