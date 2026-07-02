package com.kdlay.meaotodo.ui.board

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import com.kdlay.meaotodo.ui.ledger.formatMoney
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BoardScreen(
    viewModel: BoardViewModel,
    modifier: Modifier = Modifier
) {
    KeepScreenOn()
    val uiState by viewModel.uiState.collectAsState()
    var showModuleManager by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { BoardTopHeader(onManageModules = { showModuleManager = !showModuleManager }) }
        if (showModuleManager) {
            item { BoardModuleManagerCard(onDone = { showModuleManager = false }) }
        }
        item {
            BoxWithConstraints {
                if (maxWidth > 620.dp) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            TodayFocusCard(uiState = uiState)
                            SpendingPreviewCard(uiState = uiState)
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            PomodoroProgressCard(uiState = uiState)
                            ScheduleCard()
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        TodayFocusCard(uiState = uiState)
                        PomodoroProgressCard(uiState = uiState)
                        SpendingPreviewCard(uiState = uiState)
                        ScheduleCard()
                    }
                }
            }
        }
        item { HabitStatusCard() }
    }
}

@Composable
private fun KeepScreenOn() {
    val activity = LocalContext.current as? Activity
    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}

@Composable
private fun BoardTopHeader(onManageModules: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("MeaoToDo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("⌕", fontSize = 30.sp, color = MaterialTheme.colorScheme.onBackground)
                Text("⋮", fontSize = 28.sp, color = MaterialTheme.colorScheme.onBackground)
            }
        }
        Text("看板", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${formatBoardDate(System.currentTimeMillis())} · ☀ · 适合专注", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(
                modifier = Modifier.clickable(onClick = onManageModules),
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
            ) {
                Text(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), text = "管理模块", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DashboardCard(
    title: String,
    icon: String,
    action: String? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(38.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.68f)) {
                        Box(contentAlignment = Alignment.Center) { Text(icon, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
                    }
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Text(action ?: "⋮", color = if (action == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

@Composable
private fun TodayFocusCard(uiState: BoardUiState) {
    DashboardCard(title = "今日重点", icon = "◎", action = "查看全部任务  ›") {
        val tasks = uiState.highlightedTasks.take(3)
        if (tasks.isEmpty()) {
            Text("今天还没有重点任务。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                tasks.forEach { task -> BoardTaskItem(task = task) }
            }
        }
    }
}

@Composable
private fun BoardTaskItem(task: TaskEntity) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Surface(modifier = Modifier.padding(top = 7.dp).size(8.dp), shape = CircleShape, color = priorityColor(task.priority)) {}
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniChip(text = if (task.hasDueTime) "今天 ${formatTaskTime(task.dueAt)}" else "今天")
                MiniChip(text = priorityText(task.priority), color = priorityColor(task.priority))
            }
        }
    }
}

@Composable
private fun PomodoroProgressCard(uiState: BoardUiState) {
    val target = 6
    val done = (uiState.todayTasks.count { it.actualPomodoros > 0 } + if (uiState.activeSession != null) 1 else 0).coerceIn(0, target)
    val progress = done.toFloat() / target.toFloat()
    DashboardCard(title = "番茄进度", icon = "⏱", action = "查看专注记录  ›") {
        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("今日专注", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(done.toString(), fontSize = 46.sp, lineHeight = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(" / $target", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            ProgressRing(progress = progress)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            listOf(4, 5, 6, 2, 4, 2).forEachIndexed { index, value ->
                WeekBar(value = value, label = listOf("一", "二", "三", "四", "五", "六")[index], selected = index == 2)
            }
        }
    }
}

@Composable
private fun ProgressRing(progress: Float) {
    Box(modifier = Modifier.size(92.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(Color(0xFFE6E8F6), style = stroke)
            drawArc(
                color = Color(0xFF657AE8),
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                style = stroke
            )
        }
        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WeekBar(value: Int, label: String, selected: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Surface(
            modifier = Modifier.size(width = 18.dp, height = (18 + value * 7).dp),
            shape = RoundedCornerShape(999.dp),
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
        ) {}
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SpendingPreviewCard(uiState: BoardUiState) {
    DashboardCard(title = "支出速览", icon = "▣", action = "查看账本  ›") {
        Text("今日支出", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatMoney(uiState.todayExpenseCents), fontSize = 34.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
        Text("本周支出", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatMoney(uiState.todayExpenseCents * 3), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ScheduleCard() {
    DashboardCard(title = "日程", icon = "▦", action = "查看全部日程  ›") {
        ScheduleLine(start = "14:00", end = "15:30", title = "生物实验课", note = "实验楼 2F · 201 室", color = MaterialTheme.colorScheme.primary)
        ScheduleLine(start = "19:00", end = "20:00", title = "英语听力练习", note = "专注学习", color = MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
private fun ScheduleLine(start: String, end: String, title: String, note: String, color: Color) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.End) {
                Text(start, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(end, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = color) {}
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HabitStatusCard() {
    DashboardCard(title = "习惯 / 状态", icon = "♡", action = "记录状态  ›") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HabitTile(modifier = Modifier.weight(1f), icon = "☾", title = "睡眠", value = "7.2 小时", status = "良好", color = MaterialTheme.colorScheme.primary)
            HabitTile(modifier = Modifier.weight(1f), icon = "💧", title = "喝水", value = "5 / 8 杯", status = "进行中", color = Color(0xFF6B86FF))
            HabitTile(modifier = Modifier.weight(1f), icon = "♟", title = "运动", value = "30 分钟", status = "已达成", color = MaterialTheme.colorScheme.tertiary)
            HabitTile(modifier = Modifier.weight(1f), icon = "☺", title = "心情", value = "愉快", status = "很棒", color = Color(0xFFFFB45F))
        }
    }
}

@Composable
private fun HabitTile(modifier: Modifier, icon: String, title: String, value: String, status: String, color: Color) {
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = color.copy(alpha = 0.08f), border = BorderStroke(1.dp, color.copy(alpha = 0.22f))) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(icon, color = color, fontSize = 26.sp)
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(status, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun BoardModuleManagerCard(onDone: () -> Unit) {
    var todayVisible by rememberSaveable { mutableStateOf(true) }
    var pomodoroVisible by rememberSaveable { mutableStateOf(true) }
    var ledgerVisible by rememberSaveable { mutableStateOf(true) }
    var scheduleVisible by rememberSaveable { mutableStateOf(true) }
    var habitsVisible by rememberSaveable { mutableStateOf(true) }
    DashboardCard(title = "看板模块管理", icon = "▦", action = "完成") {
        Text("拖拽排序后续接入；当前先实现显示 / 隐藏模板。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ModuleSwitchRow("今日重点", todayVisible) { todayVisible = it }
        ModuleSwitchRow("番茄进度", pomodoroVisible) { pomodoroVisible = it }
        ModuleSwitchRow("支出速览", ledgerVisible) { ledgerVisible = it }
        ModuleSwitchRow("日程", scheduleVisible) { scheduleVisible = it }
        ModuleSwitchRow("习惯 / 状态", habitsVisible) { habitsVisible = it }
        Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onDone), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f)) {
            Text(modifier = Modifier.padding(13.dp), text = "恢复默认布局 / 收起", color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ModuleSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("☷", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun MiniChip(text: String, color: Color = MaterialTheme.colorScheme.primary) {
    Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.10f), border = BorderStroke(1.dp, color.copy(alpha = 0.22f))) {
        Text(modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp), text = text, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun priorityColor(priority: Int): Color = when (priority) {
    3 -> MaterialTheme.colorScheme.secondary
    2 -> MaterialTheme.colorScheme.primary
    1 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.outline
}

private fun priorityText(priority: Int): String = when (priority) {
    3 -> "高优先级"
    2 -> "中优先级"
    1 -> "低优先级"
    else -> "普通"
}

private fun formatBoardDate(timestamp: Long): String = SimpleDateFormat("M月d日 E", Locale.getDefault()).format(Date(timestamp))
private fun formatTaskTime(timestamp: Long?): String = timestamp?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it)) } ?: ""
