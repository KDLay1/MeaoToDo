package com.kdlay.meaotodo.ui.board

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import com.kdlay.meaotodo.ui.ledger.formatMoney

@Composable
fun BoardScreen(
    viewModel: BoardViewModel,
    modifier: Modifier = Modifier
) {
    KeepScreenOn()
    val uiState by viewModel.uiState.collectAsState()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF101216))
            .padding(22.dp)
    ) {
        val isWide = maxWidth > 640.dp
        if (isWide) {
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                BoardMainColumn(uiState = uiState, modifier = Modifier.weight(1.1f))
                BoardSideColumn(uiState = uiState, modifier = Modifier.weight(0.9f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                BoardMainColumn(uiState = uiState)
                BoardSideColumn(uiState = uiState)
            }
        }
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
private fun BoardMainColumn(uiState: BoardUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Meao Board", color = Color(0xFFF1EEF6), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("\u5355\u673a\u672c\u5730\u4eea\u8868\u76d8", color = Color(0xFFC9C4D0), style = MaterialTheme.typography.bodyMedium)
        }
        BoardMetricCard(
            title = "\u5f53\u524d\u4e13\u6ce8",
            value = uiState.timerTime,
            caption = uiState.timerTitle,
            meta = uiState.timerStatus,
            prominent = true
        )
        BoardMetricCard(
            title = "\u4eca\u65e5\u652f\u51fa",
            value = formatMoney(uiState.todayExpenseCents),
            caption = "\u8d26\u672c\u5df2\u63a5\u5165\u672c\u5730\u6570\u636e",
            meta = "Ledger"
        )
    }
}

@Composable
private fun BoardSideColumn(uiState: BoardUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BoardMetricCard(
            title = "\u4eca\u65e5\u4efb\u52a1",
            value = uiState.todayTasks.size.toString(),
            caption = "\u5f85\u529e\u603b\u6570 ${uiState.pendingTasks.size}",
            meta = "Todo"
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = Color(0xFF1B1D24),
            border = BorderStroke(1.dp, Color(0xFF353946))
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("\u4efb\u52a1\u6e05\u5355", color = Color(0xFFC9C4D0), fontWeight = FontWeight.SemiBold)
                if (uiState.highlightedTasks.isEmpty()) {
                    Text("\u4eca\u5929\u6ca1\u6709\u5f85\u529e\uff0c\u4fdd\u6301\u8fd9\u4e2a\u8282\u594f\u3002", color = Color(0xFFF1EEF6))
                } else {
                    uiState.highlightedTasks.forEach { task -> BoardTaskLine(task) }
                }
            }
        }
    }
}

@Composable
private fun BoardMetricCard(
    title: String,
    value: String,
    caption: String,
    meta: String,
    prominent: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth().widthIn(min = 0.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = Color(0xFF1B1D24),
        border = BorderStroke(1.dp, Color(0xFF353946))
    ) {
        Column(Modifier.padding(if (prominent) 24.dp else 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = Color(0xFFC9C4D0), fontWeight = FontWeight.SemiBold)
            Text(
                value,
                color = Color(0xFFF1EEF6),
                fontSize = if (prominent) 58.sp else 38.sp,
                lineHeight = if (prominent) 60.sp else 40.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(caption, color = Color(0xFFF1EEF6), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(meta, color = Color(0xFFACBFEB), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun BoardTaskLine(task: TaskEntity) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = Color(0xFF262A35)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("\u2022", color = Color(0xFF9ADBC5), fontWeight = FontWeight.Bold)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(task.title, color = Color(0xFFF1EEF6), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val meta = buildList {
                    if (task.priority > 0) add("P${task.priority}")
                    if (task.actualPomodoros > 0) add("${task.actualPomodoros} \u756a\u8304")
                }.joinToString(" ? ")
                if (meta.isNotBlank()) {
                    Text(meta, color = Color(0xFFC9C4D0), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
