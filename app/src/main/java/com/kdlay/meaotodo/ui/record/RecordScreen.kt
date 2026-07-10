package com.kdlay.meaotodo.ui.record

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kdlay.meaotodo.ui.board.BoardScreen
import com.kdlay.meaotodo.ui.board.BoardViewModel
import com.kdlay.meaotodo.ui.ledger.LedgerScreen
import com.kdlay.meaotodo.ui.ledger.LedgerViewModel
import com.kdlay.meaotodo.ui.settings.SettingsViewModel
import com.kdlay.meaotodo.ui.assistant.AssistantViewModel
import com.kdlay.meaotodo.domain.assistant.DailyTimelineEventType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class RecordSection { TIMELINE, LEDGER, INSIGHTS }

@Composable
fun RecordScreen(
    ledgerViewModel: LedgerViewModel,
    boardViewModel: BoardViewModel,
    settingsViewModel: SettingsViewModel,
    assistantViewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    var section by rememberSaveable { mutableStateOf(RecordSection.TIMELINE) }
    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
            FilterChip(
                selected = section == RecordSection.TIMELINE,
                onClick = { section = RecordSection.TIMELINE },
                label = { Text("时间线") }
            )
            FilterChip(
                modifier = Modifier.padding(start = 8.dp),
                selected = section == RecordSection.LEDGER,
                onClick = { section = RecordSection.LEDGER },
                label = { Text("账本") }
            )
            FilterChip(
                modifier = Modifier.padding(start = 8.dp),
                selected = section == RecordSection.INSIGHTS,
                onClick = { section = RecordSection.INSIGHTS },
                label = { Text("洞察") }
            )
        }
        when (section) {
            RecordSection.TIMELINE -> DailyTimelineScreen(assistantViewModel)
            RecordSection.LEDGER -> LedgerScreen(viewModel = ledgerViewModel)
            RecordSection.INSIGHTS -> BoardScreen(viewModel = boardViewModel, settingsViewModel = settingsViewModel)
        }
    }
}

@Composable
private fun DailyTimelineScreen(viewModel: AssistantViewModel) {
    val state by viewModel.uiState.collectAsState()
    val events = state.context?.timelineEvents.orEmpty()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("今天的记录", style = MaterialTheme.typography.headlineMedium)
            Text("任务、专注和支出按时间汇总在一起", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (events.isEmpty()) {
            item { Text("今天还没有完成记录。开始一个任务或记录一笔支出后会出现在这里。") }
        } else {
            items(events, key = { it.id }) { event ->
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(
                            when (event.type) {
                                DailyTimelineEventType.TASK_COMPLETED -> "✓"
                                DailyTimelineEventType.FOCUS_COMPLETED -> "◷"
                                DailyTimelineEventType.EXPENSE -> "¥"
                            }
                        )
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(event.title)
                            Text(event.detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(event.occurredAt)))
                            event.amountCents?.let { Text("¥${it / 100.0}") }
                        }
                    }
                }
            }
        }
    }
}
