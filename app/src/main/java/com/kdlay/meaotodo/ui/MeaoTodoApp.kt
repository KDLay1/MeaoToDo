package com.kdlay.meaotodo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdlay.meaotodo.ui.timer.PomodoroScreen
import com.kdlay.meaotodo.ui.timer.PomodoroViewModel
import com.kdlay.meaotodo.ui.todo.TodoScreen
import com.kdlay.meaotodo.ui.todo.TodoViewModel

private enum class MainTab(val label: String, val icon: String) {
    Today("今日", "今"),
    Timer("番茄钟", "25"),
    Ledger("账本", "账"),
    Board("看板", "板")
}

private val mainTabs = MainTab.entries.toList()

@Composable
fun MeaoTodoApp(
    todoViewModel: TodoViewModel,
    pomodoroViewModel: PomodoroViewModel
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Today) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                mainTabs.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Text(tab.icon, fontWeight = FontWeight.SemiBold) },
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
                MainTab.Today -> TodoScreen(viewModel = todoViewModel)
                MainTab.Timer -> PomodoroScreen(viewModel = pomodoroViewModel)
                MainTab.Ledger -> LedgerScreen()
                MainTab.Board -> BoardScreen()
            }
        }
    }
}

@Composable
private fun LedgerScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("账本", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        DashboardCard(title = "今日", content = "0.00", caption = "还没有记录支出")
        DashboardCard(title = "本月", content = "预算等待设置", caption = "分类统计将在这里显示")
    }
}

@Composable
private fun BoardScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF111318))
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("看板模式", color = Color(0xFFF1EEF6), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("备用机低亮度桌面视图", color = Color(0xFFC9C4D0))
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color(0xFF244D42),
                contentColor = Color(0xFFC6F3E4)
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    text = "Wi-Fi",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        BoardCard(title = "当前专注", content = "暂无任务", meta = "等待主力机同步")
        BoardCard(title = "番茄钟", content = "25:00", meta = "Ready")
        BoardCard(title = "今日支出", content = "0.00", meta = "账本模块稍后接入")
    }
}

@Composable
private fun DashboardCard(title: String, content: String, caption: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(content, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BoardCard(title: String, content: String, meta: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF1B1D24),
        border = BorderStroke(1.dp, Color(0xFF353946))
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = Color(0xFFC9C4D0), fontWeight = FontWeight.SemiBold)
            Text(content, color = Color(0xFFF1EEF6), fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Text(meta, color = Color(0xFFACBFEB))
        }
    }
}
