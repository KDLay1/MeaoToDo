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

private enum class MainTab(val label: String) {
    Today("Today"),
    Timer("Timer"),
    Ledger("Ledger"),
    Board("Board")
}

@Composable
fun MeaoTodoApp() {
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
            MainTab.Today -> TodayScreen(Modifier.padding(innerPadding))
            MainTab.Timer -> TimerScreen(Modifier.padding(innerPadding))
            MainTab.Ledger -> LedgerScreen(Modifier.padding(innerPadding))
            MainTab.Board -> BoardScreen(Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun TodayScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("MeaoToDo", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Local-first Todo, Pomodoro and Ledger for a two-phone workflow.")
        DashboardCard(title = "Today", content = "No tasks yet. Next step: implement Room-backed Todo list.")
        DashboardCard(title = "Wi‑Fi Sync", content = "Discovery and pairing interfaces are prepared under sync/.")
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
        Text("Pomodoro state machine will be implemented here.")
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
        Text("Ledger", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        DashboardCard(title = "Today", content = "¥0.00")
        DashboardCard(title = "Month", content = "Budget and category statistics will appear here.")
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
            Text("Board Mode", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Wi‑Fi")
        }
        DashboardCard(title = "Current Focus", content = "No active task")
        DashboardCard(title = "Pomodoro", content = "25:00")
        DashboardCard(title = "Today Spending", content = "¥0.00")
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
