package com.kdlay.meaotodo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdlay.meaotodo.ui.todo.TodoScreen
import com.kdlay.meaotodo.ui.todo.TodoViewModel

private enum class MainTab(val label: String) {
    Today("今日"),
    Timer("番茄钟"),
    Ledger("账本"),
    Board("看板")
}

@Composable
fun MeaoTodoApp(todoViewModel: TodoViewModel) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Today) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Text(tab.label.take(1)) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            MainTab.Today -> TodoScreen(
                viewModel = todoViewModel,
                modifier = Modifier.padding(innerPadding)
            )
            MainTab.Timer -> TimerScreen(Modifier.padding(innerPadding))
            MainTab.Ledger -> LedgerScreen(Modifier.padding(innerPadding))
            MainTab.Board -> BoardScreen(Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun TimerScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("25:00", fontSize = 72.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("番茄钟状态机将在这里实现。")
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
        DashboardCard(title = "今日", content = "¥0.00")
        DashboardCard(title = "本月", content = "预算和分类统计将在这里显示。")
    }
}

@Composable
private fun BoardScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("看板模式", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Wi‑Fi")
        }
        DashboardCard(title = "当前专注", content = "暂无进行中的任务")
        DashboardCard(title = "番茄钟", content = "25:00")
        DashboardCard(title = "今日支出", content = "¥0.00")
    }
}

@Composable
private fun DashboardCard(title: String, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(content, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
