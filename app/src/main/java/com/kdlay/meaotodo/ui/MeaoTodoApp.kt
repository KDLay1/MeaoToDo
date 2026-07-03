package com.kdlay.meaotodo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kdlay.meaotodo.ui.board.BoardScreen
import com.kdlay.meaotodo.ui.board.BoardViewModel
import com.kdlay.meaotodo.ui.ledger.LedgerScreen
import com.kdlay.meaotodo.ui.ledger.LedgerViewModel
import com.kdlay.meaotodo.ui.settings.SettingsShellScreen
import com.kdlay.meaotodo.ui.timer.PomodoroTemplateScreen
import com.kdlay.meaotodo.ui.timer.PomodoroViewModel
import com.kdlay.meaotodo.ui.todo.TodoScreen
import com.kdlay.meaotodo.ui.todo.TodoViewModel

private enum class MainTab(val label: String, val icon: String) {
    Today("今日", "☷"),
    Timer("番茄", "⏱"),
    Ledger("账本", "▣"),
    Board("看板", "▤"),
    Settings("设置", "设")
}

private val mainTabs = MainTab.entries.toList()

@Composable
fun MeaoTodoApp(
    todoViewModel: TodoViewModel,
    pomodoroViewModel: PomodoroViewModel,
    ledgerViewModel: LedgerViewModel,
    boardViewModel: BoardViewModel,
    onTimerImmersiveModeChange: (Boolean) -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Today) }
    var previousContentTab by rememberSaveable { mutableStateOf(MainTab.Today) }
    var isTimerImmersive by rememberSaveable { mutableStateOf(false) }
    var requestedPomodoroTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    val hideBottomBar = selectedTab == MainTab.Timer && isTimerImmersive

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!hideBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    mainTabs.forEach { tab ->
                        val selected = selectedTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (tab != MainTab.Settings) {
                                    previousContentTab = tab
                                }
                                selectedTab = tab
                            },
                            icon = { MainTabIcon(tab = tab, selected = selected) },
                            label = { Text(tab.label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
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
            when (selectedTab) {
                MainTab.Today -> TodoScreen(
                    viewModel = todoViewModel,
                    onStartFocus = { task ->
                        requestedPomodoroTaskId = task.id
                        previousContentTab = MainTab.Timer
                        selectedTab = MainTab.Timer
                    }
                )
                MainTab.Timer -> PomodoroTemplateScreen(
                    viewModel = pomodoroViewModel,
                    requestedStartTaskId = requestedPomodoroTaskId,
                    onRequestedStartTaskHandled = { requestedPomodoroTaskId = null },
                    onImmersiveModeChange = { isImmersive ->
                        isTimerImmersive = isImmersive
                        onTimerImmersiveModeChange(isImmersive)
                    }
                )
                MainTab.Ledger -> LedgerScreen(viewModel = ledgerViewModel)
                MainTab.Board -> BoardScreen(viewModel = boardViewModel)
                MainTab.Settings -> SettingsShellScreen(onBack = { selectedTab = previousContentTab })
            }
        }
    }
}

@Composable
private fun MainTabIcon(tab: MainTab, selected: Boolean) {
    Surface(
        modifier = Modifier
            .width(56.dp)
            .height(34.dp),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = tab.icon,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
