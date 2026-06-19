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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TodoDisplayMode.entries.forEach { mode ->
            CompactModePill(
                text = mode.label,
                selected = displayMode == mode,
                onClick = { onDisplayModeChange(mode) }
            )
        }
        if (displayMode == TodoDisplayMode.Calendar) {
            TodoCalendarMode.entries.forEach { mode ->
                CompactModePill(
                    text = mode.label,
                    selected = calendarMode == mode,
                    onClick = { onCalendarModeChange(mode) }
                )
            }
        }
    }
}

@Composable
private fun CompactModePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.36f))
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
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
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    val weekEnd = addDays(weekStart, 7)
    val weekTasks = tasks.filter { task -> task.dueAt?.let { it >= weekStart && it < weekEnd } == true }
    val scrollState = rememberScrollState()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CalendarNavigationHeader(
            title = "${formatDate(weekStart)} - ${formatDate(addDays(weekStart, 6))}",
            subtitle = "本周 ${weekTasks.size} 项 · 未完成 ${weekTasks.count { !it.isDone }} 项",
            onPrevious = { onSelectedDateChange(addDays(weekStart, -7)) },
            onToday = { onSelectedDateChange(System.currentTimeMillis()) },
            onNext = { onSelectedDateChange(addDays(weekStart, 7)) }
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
        ) {
            Column(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("时间", modifier = Modifier.width(50.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    weekDays.forEach { day ->
                        DayHeaderCell(
                            day = day,
                            selectedDate = selectedDate,
                            tasks = tasksForDay(tasks, day),
                            onClick = { onSelectedDateChange(day) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = "全天",
                        modifier = Modifier.width(50.dp).padding(top = 8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    weekDays.forEach { day ->
                        val dayTasks = tasksForDay(tasks, day).filterNot { it.hasDueTime }
                        WeekTaskCell(tasks = dayTasks, onEdit = onEdit, emphasized = dayTasks.isNotEmpty())
                    }
                }

                (0..23).forEach { hour ->
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = "%02d:00".format(hour),
                            modifier = Modifier.width(50.dp).padding(top = 8.dp),
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
}

@Composable
private fun DayHeaderCell(
    day: Long,
    selectedDate: Long,
    tasks: List<TaskEntity>,
    onClick: () -> Unit
) {
    val selected = startOfDay(day) == startOfDay(selectedDate)
    val today = isToday(day)
    val container = when {
        selected -> MaterialTheme.colorScheme.primaryContainer
        today -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
    }
    val content = when {
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        today -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier.width(100.dp).clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = content,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(weekdayLabel(day), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                Text(dayOfMonth(day).toString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Text("${tasks.size}", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun WeekTaskCell(
    tasks: List<TaskEntity>,
    onEdit: (TaskEntity) -> Unit,
    emphasized: Boolean = false
) {
    Surface(
        modifier = Modifier.width(100.dp).heightIn(min = 52.dp),
        shape = MaterialTheme.shapes.medium,
        color = if (emphasized) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.56f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
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
    val priorityMark = if (task.priority > 0) " P${task.priority}" else ""
    Text(
        text = "$prefix${task.title}$priorityMark",
        modifier = Modifier.clickable(onClick = onClick),
        style = MaterialTheme.typography.labelSmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
        color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
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

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CalendarNavigationHeader(
            title = monthTitle(monthStart),
            subtitle = "本月 ${tasksInMonth(tasks, monthStart).size} 项",
            onPrevious = { onSelectedDateChange(addMonths(monthStart, -1)) },
            onToday = { onSelectedDateChange(System.currentTimeMillis()) },
            onNext = { onSelectedDateChange(addMonths(monthStart, 1)) }
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
        ) {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                        Text(it, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        }

        Text("${formatDate(selectedDate)} · ${selectedTasks.size} 项", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        if (selectedTasks.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Text("这一天没有设置截止日期的任务。", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall)
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
    val completed = tasks.isNotEmpty() && tasks.all { it.isDone }

    Surface(
        modifier = modifier.heightIn(min = 66.dp).clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (inMonth) 0.44f else 0.18f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (selected) 0.28f else 0.12f))
    ) {
        Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dayOfMonth(day).toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected || isToday(day)) FontWeight.Bold else FontWeight.Normal,
                    color = if (inMonth) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                )
                if (completed) {
                    Text("✓", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                }
            }
            TaskDots(tasks = tasks, maxDots = 4)
            if (tasks.isNotEmpty()) {
                Text("${tasks.size} 项", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskDots(tasks: List<TaskEntity>, maxDots: Int) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        tasks.take(maxDots).forEach { task ->
            Surface(
                modifier = Modifier.size(if (task.priority >= 3) 7.dp else 6.dp),
                shape = CircleShape,
                color = when {
                    task.isDone -> MaterialTheme.colorScheme.tertiary
                    task.priority >= 3 -> MaterialTheme.colorScheme.error
                    task.priority == 2 -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.primary
                }
            ) {}
        }
        if (tasks.size > maxDots) {
            Text("+${tasks.size - maxDots}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CalendarNavigationHeader(
            title = "${year} 年",
            subtitle = "全年 ${tasksInYear(tasks, year).size} 项",
            onPrevious = { onSelectedDateChange(startOfYear(year - 1)) },
            onToday = { onSelectedDateChange(System.currentTimeMillis()) },
            onNext = { onSelectedDateChange(startOfYear(year + 1)) }
        )
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (0..3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0..2).forEach { column ->
                        val month = row * 3 + column
                        YearMonthPreview(
                            monthStart = startOfMonth(year, month),
                            tasks = tasks,
                            onClick = {
                                onSelectedDateChange(startOfMonth(year, month))
                                onCalendarModeChange(TodoCalendarMode.Month)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YearMonthPreview(
    monthStart: Long,
    tasks: List<TaskEntity>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gridStart = startOfWeek(monthStart)
    val monthTasks = tasksInMonth(tasks, monthStart)
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${monthOf(monthStart) + 1}月", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text("${monthTasks.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            (0..5).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    (0..6).forEach { offset ->
                        val day = addDays(gridStart, week * 7 + offset)
                        val dayTasks = tasksForDay(tasks, day)
                        val active = monthOf(day) == monthOf(monthStart) && dayTasks.isNotEmpty()
                        Surface(
                            modifier = Modifier.height(6.dp).weight(1f),
                            shape = CircleShape,
                            color = when {
                                !active -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                                dayTasks.any { !it.isDone && it.priority >= 3 } -> MaterialTheme.colorScheme.error
                                dayTasks.all { it.isDone } -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.primary
                            }
                        ) {}
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarNavigationHeader(
    title: String,
    subtitle: String,
    onPrevious: () -> Unit,
    onToday: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            CalendarNavPill(text = "‹", onClick = onPrevious)
            CalendarNavPill(text = "今天", onClick = onToday)
            CalendarNavPill(text = "›", onClick = onNext)
        }
    }
}

@Composable
private fun CalendarNavPill(text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.32f))
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun tasksInMonth(tasks: List<TaskEntity>, monthStart: Long): List<TaskEntity> {
    val monthEnd = addMonths(monthStart, 1)
    return tasks.filter { task -> task.dueAt?.let { it >= monthStart && it < monthEnd } == true }
}

private fun tasksInYear(tasks: List<TaskEntity>, year: Int): List<TaskEntity> {
    val yearStart = startOfYear(year)
    val yearEnd = startOfYear(year + 1)
    return tasks.filter { task -> task.dueAt?.let { it >= yearStart && it < yearEnd } == true }
}
