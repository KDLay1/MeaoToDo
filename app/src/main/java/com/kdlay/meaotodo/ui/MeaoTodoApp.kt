package com.kdlay.meaotodo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class MainTab(val label: String, val icon: String) {
    Today("Today", "✓"),
    Timer("Timer", "25"),
    Ledger("Ledger", "¥"),
    Board("Board", "□")
}

private data class TodoListTab(
    val label: String,
    val count: Int
)

private data class TodoGroup(
    val title: String,
    val subtitle: String,
    val tasks: List<UiTask>
)

private data class UiTask(
    val id: String,
    val title: String,
    val note: String,
    val listName: String,
    val dueGroup: String,
    val dueLabel: String,
    val priority: Int,
    val pomodoros: String,
    val isDone: Boolean = false
)

@Composable
fun MeaoTodoApp() {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Today) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Text(
                                text = tab.icon,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                MainTab.Today -> TodoPolishScreen()
                MainTab.Timer -> TimerScreen()
                MainTab.Ledger -> LedgerScreen()
                MainTab.Board -> BoardScreen()
            }
        }
    }
}

@Composable
private fun TodoPolishScreen() {
    var selectedList by rememberSaveable { mutableStateOf("Today") }
    var showCompleted by rememberSaveable { mutableStateOf(false) }
    val tasks = remember { sampleTasks() }
    val visibleTasks = remember(selectedList, tasks) {
        when (selectedList) {
            "Today" -> tasks.filter { it.dueGroup == "Today" }
            "All" -> tasks
            else -> tasks.filter { it.listName == selectedList }
        }
    }
    val activeTasks = visibleTasks.filterNot { it.isDone }
    val completedTasks = visibleTasks.filter { it.isDone }
    val tabs = remember(tasks) {
        listOf(
            TodoListTab("Today", tasks.count { it.dueGroup == "Today" && !it.isDone }),
            TodoListTab("Inbox", tasks.count { it.listName == "Inbox" && !it.isDone }),
            TodoListTab("Study", tasks.count { it.listName == "Study" && !it.isDone }),
            TodoListTab("Life", tasks.count { it.listName == "Life" && !it.isDone }),
            TodoListTab("Project", tasks.count { it.listName == "Project" && !it.isDone }),
            TodoListTab("All", tasks.count { !it.isDone })
        )
    }
    val groups = remember(activeTasks) {
        listOf(
            TodoGroup("Today", "Tasks that need attention before the day ends", activeTasks.filter { it.dueGroup == "Today" }),
            TodoGroup("Upcoming", "Not urgent, but already visible", activeTasks.filter { it.dueGroup == "Upcoming" }),
            TodoGroup("No date", "Collected ideas waiting for scheduling", activeTasks.filter { it.dueGroup == "No date" })
        ).filter { it.tasks.isNotEmpty() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            TodayHero(
                remainingCount = activeTasks.size,
                completedCount = completedTasks.size
            )
        }
        item {
            ListSwitcher(
                tabs = tabs,
                selectedLabel = selectedList,
                onSelected = { selectedList = it }
            )
        }
        item { QuickAddBar() }
        item { FocusTaskCard(task = activeTasks.firstOrNull()) }

        if (groups.isEmpty()) {
            item { EmptyTodoState(selectedList = selectedList) }
        } else {
            groups.forEach { group ->
                item {
                    SectionHeader(
                        title = group.title,
                        subtitle = group.subtitle
                    )
                }
                items(group.tasks, key = { it.id }) { task ->
                    TaskRow(task = task)
                }
            }
        }

        item {
            CompletedToggle(
                count = completedTasks.size,
                expanded = showCompleted,
                onClick = { showCompleted = !showCompleted }
            )
        }
        items(
            items = if (showCompleted) completedTasks else emptyList(),
            key = { it.id }
        ) { task ->
            TaskRow(task = task)
        }
    }
}

@Composable
private fun TodayHero(
    remainingCount: Int,
    completedCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "MeaoToDo",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "A softer todo board for today.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f)
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        text = "Wi‑Fi ready",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniStat(
                    modifier = Modifier.weight(1f),
                    value = remainingCount.toString(),
                    label = "open"
                )
                MiniStat(
                    modifier = Modifier.weight(1f),
                    value = completedCount.toString(),
                    label = "done"
                )
                MiniStat(
                    modifier = Modifier.weight(1f),
                    value = "25m",
                    label = "next focus"
                )
            }
        }
    }
}

@Composable
private fun MiniStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.56f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.68f)
            )
        }
    }
}

@Composable
private fun ListSwitcher(
    tabs: List<TodoListTab>,
    selectedLabel: String,
    onSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        tabs.forEach { tab ->
            ListChip(
                label = tab.label,
                count = tab.count,
                selected = tab.label == selectedLabel,
                onClick = { onSelected(tab.label) }
            )
        }
        AddListChip()
    }
}

@Composable
private fun ListChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Surface(
                shape = CircleShape,
                color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AddListChip() {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
            text = "+ List",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun QuickAddBar() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Add a small thing to do",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Title first, details later",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FocusTaskCard(task: UiTask?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("25", fontWeight = FontWeight.Bold)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Next focus",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f)
                )
                Text(
                    text = task?.title ?: "No active task",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "Start",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TaskRow(task: UiTask) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = MaterialTheme.shapes.large,
        color = if (task.isDone) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (task.isDone) 0.22f else 0.36f)),
        shadowElevation = if (task.isDone) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CompleteCircle(done = task.isDone)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                        color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    if (task.priority > 0) {
                        PriorityBadge(priority = task.priority)
                    }
                }
                if (task.note.isNotBlank()) {
                    Text(
                        text = task.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DueBadge(label = task.dueLabel)
                    Text(
                        text = task.listName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "·",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = task.pomodoros,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CompleteCircle(done: Boolean) {
    Surface(
        modifier = Modifier.size(25.dp),
        shape = CircleShape,
        color = if (done) MaterialTheme.colorScheme.tertiary else Color.Transparent,
        contentColor = if (done) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.outline,
        border = if (done) null else BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (done) {
                Text("✓", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PriorityBadge(priority: Int) {
    val color = when (priority) {
        3 -> MaterialTheme.colorScheme.errorContainer
        2 -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = when (priority) {
        3 -> MaterialTheme.colorScheme.error
        2 -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color,
        contentColor = contentColor
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            text = "P$priority",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DueBadge(label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EmptyTodoState(selectedList: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("☾", fontSize = 32.sp)
            Text(
                text = "$selectedList is clean.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Add one small task when something comes to mind.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CompletedToggle(
    count: Int,
    expanded: Boolean,
    onClick: () -> Unit
) {
    if (count == 0) return

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.54f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Completed · $count",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(if (expanded) "Hide" else "Show", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun TimerScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 36.dp, vertical = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("25:00", fontSize = 72.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "Ready for a gentle focus session",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Start Focus")
                }
            }
        }
    }
}

@Composable
private fun LedgerScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Ledger", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        DashboardCard(title = "Today", content = "¥0.00", caption = "No spending recorded yet")
        DashboardCard(title = "Month", content = "Budget is waiting", caption = "Category statistics will appear here")
        DashboardCard(title = "Quick Entry", content = "+ Add expense", caption = "Keep the ledger light and fast")
    }
}

@Composable
private fun BoardScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111318))
            .padding(28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Board Mode", color = Color(0xFFF1EEF6), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Low-brightness desk view", color = Color(0xFFC9C4D0))
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFF244D42),
                    contentColor = Color(0xFFC6F3E4)
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        text = "Synced",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            BoardCard(title = "Current Focus", content = "Review UI polish", meta = "25 min · Study")
            BoardCard(title = "Pomodoro", content = "25:00", meta = "Ready")
            BoardCard(title = "Today Spending", content = "¥0.00", meta = "Month budget quiet")
        }
    }
}

@Composable
private fun DashboardCard(
    title: String,
    content: String,
    caption: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(content, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BoardCard(
    title: String,
    content: String,
    meta: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF1B1D24),
        border = BorderStroke(1.dp, Color(0xFF353946))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, color = Color(0xFFC9C4D0), fontWeight = FontWeight.SemiBold)
            Text(content, color = Color(0xFFF1EEF6), fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Text(meta, color = Color(0xFFACBFEB))
        }
    }
}

private fun sampleTasks(): List<UiTask> = listOf(
    UiTask(
        id = "task-1",
        title = "Polish the Todo main screen",
        note = "Keep it close to TickTick's clarity, but softer.",
        listName = "Project",
        dueGroup = "Today",
        dueLabel = "Tonight",
        priority = 3,
        pomodoros = "2 pomodoros"
    ),
    UiTask(
        id = "task-2",
        title = "Review repository structure after Codex test",
        note = "Focus on UI files and avoid repository changes.",
        listName = "Project",
        dueGroup = "Today",
        dueLabel = "Today",
        priority = 2,
        pomodoros = "1 pomodoro"
    ),
    UiTask(
        id = "task-3",
        title = "Read statistical physics notes",
        note = "Only keep the next concrete chapter here.",
        listName = "Study",
        dueGroup = "Today",
        dueLabel = "20:30",
        priority = 1,
        pomodoros = "2 pomodoros"
    ),
    UiTask(
        id = "task-4",
        title = "Prepare board-mode content rules",
        note = "Large text, low brightness, no dense controls.",
        listName = "Project",
        dueGroup = "Upcoming",
        dueLabel = "Tomorrow",
        priority = 1,
        pomodoros = "1 pomodoro"
    ),
    UiTask(
        id = "task-5",
        title = "Buy daily supplies",
        note = "Keep life tasks separate from study tasks.",
        listName = "Life",
        dueGroup = "No date",
        dueLabel = "No date",
        priority = 0,
        pomodoros = "quick"
    ),
    UiTask(
        id = "task-6",
        title = "Archive old scratch tasks",
        note = "Completed items should be quiet, not noisy.",
        listName = "Inbox",
        dueGroup = "Today",
        dueLabel = "Done",
        priority = 0,
        pomodoros = "done",
        isDone = true
    )
)
