package com.kdlay.meaotodo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kdlay.meaotodo.ui.assistant.AssistantScreen
import com.kdlay.meaotodo.ui.assistant.AssistantViewModel
import com.kdlay.meaotodo.ui.board.BoardViewModel
import com.kdlay.meaotodo.ui.ledger.LedgerViewModel
import com.kdlay.meaotodo.ui.plan.PlanScreen
import com.kdlay.meaotodo.ui.record.RecordScreen
import com.kdlay.meaotodo.ui.settings.SettingsShellScreen
import com.kdlay.meaotodo.ui.settings.SettingsViewModel
import com.kdlay.meaotodo.ui.timer.FocusScreen
import com.kdlay.meaotodo.ui.timer.PomodoroViewModel
import com.kdlay.meaotodo.ui.todo.TodoViewModel

private enum class MainTab(val label: String, val icon: ImageVector) {
    Today("今天", Icons.Filled.Home),
    Plan("计划", Icons.AutoMirrored.Filled.List),
    Focus("专注", Icons.Filled.PlayArrow),
    Record("记录", Icons.Filled.DateRange)
}

@Composable
fun MeaoTodoApp(
    todoViewModel: TodoViewModel,
    pomodoroViewModel: PomodoroViewModel,
    ledgerViewModel: LedgerViewModel,
    boardViewModel: BoardViewModel,
    settingsViewModel: SettingsViewModel,
    assistantViewModel: AssistantViewModel,
    onTimerImmersiveModeChange: (Boolean) -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Today) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var isTimerImmersive by rememberSaveable { mutableStateOf(false) }
    var requestedPomodoroTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    val assistantState by assistantViewModel.uiState.collectAsState()
    val hideChrome = isTimerImmersive && selectedTab == MainTab.Focus

    fun openFocus(taskId: String?) {
        requestedPomodoroTaskId = taskId
        selectedTab = MainTab.Focus
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!hideChrome && !showSettings) {
                Column(
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    assistantState.context?.activeFocus?.let { focus ->
                        ActiveFocusDock(
                            title = focus.title,
                            onOpen = { selectedTab = MainTab.Focus }
                        )
                    }
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        MainTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { selectedTab = tab },
                                icon = { MainTabIcon(tab, selected) },
                                label = {
                                    Text(
                                        tab.label,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
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
            if (showSettings) {
                SettingsShellScreen(viewModel = settingsViewModel, onBack = { showSettings = false })
            } else {
                when (selectedTab) {
                    MainTab.Today -> AssistantScreen(
                        viewModel = assistantViewModel,
                        onOpenSettings = { showSettings = true },
                        onOpenPlan = { selectedTab = MainTab.Plan },
                        onOpenRecord = { selectedTab = MainTab.Record },
                        onStartFocus = ::openFocus
                    )
                    MainTab.Plan -> PlanScreen(
                        todoViewModel = todoViewModel,
                        onRequestTaskFocus = ::openFocus
                    )
                    MainTab.Focus -> FocusScreen(
                        viewModel = pomodoroViewModel,
                        requestedStartTaskId = requestedPomodoroTaskId,
                        onRequestedStartTaskHandled = { requestedPomodoroTaskId = null },
                        onImmersiveModeChange = { immersive ->
                            isTimerImmersive = immersive
                            onTimerImmersiveModeChange(immersive)
                        }
                    )
                    MainTab.Record -> RecordScreen(
                        ledgerViewModel = ledgerViewModel,
                        boardViewModel = boardViewModel,
                        settingsViewModel = settingsViewModel,
                        assistantViewModel = assistantViewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveFocusDock(title: String, onOpen: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.76f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "正在专注",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                "返回计时器",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MainTabIcon(tab: MainTab, selected: Boolean) {
    Surface(
        modifier = Modifier.width(50.dp).height(32.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (selected) {
                    Surface(
                        modifier = Modifier.width(28.dp).height(3.dp),
                        shape = RoundedCornerShape(99.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {}
                }
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    modifier = Modifier.padding(top = if (selected) 5.dp else 8.dp)
                )
            }
        }
    }
}
